package com.example.template.architecture

import com.tngtech.archunit.core.domain.JavaClass
import com.tngtech.archunit.core.importer.ImportOption
import com.tngtech.archunit.junit.AnalyzeClasses
import com.tngtech.archunit.junit.ArchTest
import com.tngtech.archunit.lang.ArchCondition
import com.tngtech.archunit.lang.ArchRule
import com.tngtech.archunit.lang.ConditionEvents
import com.tngtech.archunit.lang.SimpleConditionEvent
import com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes
import com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses
import com.tngtech.archunit.library.dependencies.SlicesRuleDefinition.slices

/**
 * 기능(feature) 패키지마다 domain <- application <- adapter 방향만 허용.
 * ArchUnit 은 컴파일된 바이트코드를 보므로 코틀린 코드에도 자바 버전 규칙이 그대로 적용된다.
 *
 * 코틀린이라 달라진 점
 * - @ArchTest 필드는 JUnit 이 static 필드로 찾는다. companion object 안에 @JvmField 로 둔다
 * - companion object 는 ListItemsQuery$Companion 같은 중첩 클래스가 된다. portsAreInterfaces 는 최상위 클래스만 본다
 */
@AnalyzeClasses(packages = ["com.example.template"], importOptions = [ImportOption.DoNotIncludeTests::class])
class HexagonalArchitectureTest {

    companion object {
        private const val ROOT = "com.example.template"
        private val FEATURE = Regex(Regex.escape(ROOT) + "\\.([^.]+)\\..*")

        @ArchTest
        @JvmField
        val domainDependsOnNothingOutside: ArchRule = noClasses()
            .that().resideInAPackage("..domain..")
            .should().dependOnClassesThat().resideInAnyPackage("..application..", "..adapter..")

        @ArchTest
        @JvmField
        val domainIsFrameworkFree: ArchRule = noClasses()
            .that().resideInAPackage("..domain..")
            .should().dependOnClassesThat().resideInAnyPackage(
                "org.springframework..", "jakarta.persistence..", "jakarta.validation..", "org.jooq..",
            )

        @ArchTest
        @JvmField
        val applicationDoesNotDependOnAdapters: ArchRule = noClasses()
            .that().resideInAPackage("..application..")
            .should().dependOnClassesThat().resideInAPackage("..adapter..")

        @ArchTest
        @JvmField
        val applicationDoesNotUseWebOrPersistence: ArchRule = noClasses()
            .that().resideInAPackage("..application..")
            .should().dependOnClassesThat().resideInAnyPackage(
                "org.springframework.web..", "org.springframework.data..", "jakarta.persistence..", "org.jooq..",
            )

        // 인증 정보(SecurityContextHolder, Authentication, Jwt 등)는 adapter.in 에서 꺼내 Command 에 사용자 id 로 넘긴다.
        // 서비스와 도메인은 Spring Security 를 모른다 (docs/authentication.md). domain 은 위 domainIsFrameworkFree 가 막는다
        @ArchTest
        @JvmField
        val applicationDoesNotUseSecurity: ArchRule = noClasses()
            .that().resideInAPackage("..application..")
            .should().dependOnClassesThat().resideInAPackage("org.springframework.security..")
            .because("인증 정보는 adapter.in 에서 꺼내 Command 로 넘긴다 (docs/authentication.md)")

        @ArchTest
        @JvmField
        val inboundAdaptersDoNotUseOutbound: ArchRule = noClasses()
            .that().resideInAPackage("..adapter.in..")
            .should().dependOnClassesThat().resideInAPackage("..adapter.out..")

        @ArchTest
        @JvmField
        val outboundAdaptersDoNotUseInbound: ArchRule = noClasses()
            .that().resideInAPackage("..adapter.out..")
            .should().dependOnClassesThat().resideInAPackage("..adapter.in..")

        @ArchTest
        @JvmField
        val inboundAdaptersUseOnlyInputPorts: ArchRule = noClasses()
            .that().resideInAPackage("..adapter.in..")
            .should().dependOnClassesThat().resideInAnyPackage("..application.service..", "..application.port.out..")

        @ArchTest
        @JvmField
        val portsAreInterfaces: ArchRule = classes()
            .that().resideInAnyPackage("..application.port.in..", "..application.port.out..")
            .and().areTopLevelClasses()
            .and().haveSimpleNameNotEndingWith("Command")
            .and().haveSimpleNameNotEndingWith("Query")
            .and().haveSimpleNameNotEndingWith("Result")
            .should().beInterfaces()

        @ArchTest
        @JvmField
        val onlyOutboundAdaptersUseJooq: ArchRule = noClasses()
            .that().resideOutsideOfPackages("..adapter.out..", "$ROOT.common.jooq..")
            .should().dependOnClassesThat().resideInAnyPackage("org.jooq..", "$ROOT.common.jooq..")

        @ArchTest
        @JvmField
        val commonDoesNotDependOnFeatures: ArchRule = classes()
            .that().resideInAPackage("$ROOT.common..")
            .should(notDependOnAnyFeature())

        @ArchTest
        @JvmField
        val featuresAreFreeOfCycles: ArchRule = slices()
            .matching("$ROOT.(*)..")
            .should().beFreeOfCycles()

        // 다른 기능은 application.port.in(유스케이스)과 domain 만 사용할 수 있다
        @ArchTest
        @JvmField
        val featuresTalkThroughInputPorts: ArchRule = classes()
            .that().resideInAPackage("$ROOT..")
            .should(onlyUseOtherFeaturesThroughInputPorts())

        private fun featureOf(javaClass: JavaClass): String? {
            val feature = FEATURE.matchEntire(javaClass.name)?.groupValues?.get(1)
            return feature?.takeIf { it != "common" }
        }

        // object : 타입 { } 은 익명 클래스 (자바의 new ArchCondition<>() { ... })
        private fun notDependOnAnyFeature() = object : ArchCondition<JavaClass>("not depend on any feature package") {
            override fun check(item: JavaClass, events: ConditionEvents) {
                item.directDependenciesFromSelf
                    .filter { featureOf(it.targetClass) != null }
                    .forEach { events.add(SimpleConditionEvent.violated(it, it.description)) }
            }
        }

        private fun onlyUseOtherFeaturesThroughInputPorts() =
            object : ArchCondition<JavaClass>("only use other features through application.port.in or domain") {
                override fun check(item: JavaClass, events: ConditionEvents) {
                    val from = featureOf(item) ?: return
                    for (d in item.directDependenciesFromSelf) {
                        val to = featureOf(d.targetClass)
                        if (to == null || to == from) continue
                        val pkg = d.targetClass.packageName
                        if (!pkg.contains(".application.port.in") && !pkg.contains(".domain")) {
                            events.add(SimpleConditionEvent.violated(d, d.description))
                        }
                    }
                }
            }
    }
}
