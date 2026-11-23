package com.example.template.architecture

import com.tngtech.archunit.base.DescribedPredicate
import com.tngtech.archunit.core.domain.JavaClass
import com.tngtech.archunit.core.domain.JavaMethodCall
import com.tngtech.archunit.core.domain.JavaModifier
import com.tngtech.archunit.core.importer.ImportOption
import com.tngtech.archunit.junit.AnalyzeClasses
import com.tngtech.archunit.junit.ArchTest
import com.tngtech.archunit.lang.ArchCondition
import com.tngtech.archunit.lang.ArchRule
import com.tngtech.archunit.lang.ConditionEvents
import com.tngtech.archunit.lang.SimpleConditionEvent
import com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes
import com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses
import com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noMethods
import com.tngtech.archunit.library.dependencies.SlicesRuleDefinition.slices
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.MonthDay
import java.time.OffsetDateTime
import java.time.OffsetTime
import java.time.Year
import java.time.YearMonth
import java.time.ZonedDateTime
import kotlin.reflect.KVisibility

/**
 * 기능(feature) 패키지마다 domain <- application <- adapter 방향만 허용.
 * ArchUnit 은 컴파일된 바이트코드를 보므로 코틀린 코드에도 자바 버전 규칙이 그대로 적용된다.
 *
 * 코틀린이라 달라진 점
 * - @ArchTest 필드는 JUnit 이 static 필드로 찾는다. companion object 안에 @JvmField 로 둔다
 * - companion object 는 ListItemsQuery$Companion 같은 중첩 클래스가 된다. portsAreInterfaces 는 최상위 클래스만 본다
 * - internal 은 바이트코드에서 public 이라 ArchUnit 의 notBePublic() 으로 못 잡는다. kotlin-reflect 로 가시성을 읽는다
 */
@AnalyzeClasses(packages = ["com.example.template"], importOptions = [ImportOption.DoNotIncludeTests::class])
class HexagonalArchitectureTest {

    companion object {
        private const val ROOT = "com.example.template"
        private val FEATURE = Regex(Regex.escape(ROOT) + "\\.([^.]+)\\..*")
        private const val SPRING_TRANSACTIONAL = "org.springframework.transaction.annotation.Transactional"
        private const val JAKARTA_TRANSACTIONAL = "jakarta.transaction.Transactional"
        private val TIME_TYPES = setOf(
            Instant::class.java, LocalDate::class.java, LocalDateTime::class.java, LocalTime::class.java,
            OffsetDateTime::class.java, OffsetTime::class.java, ZonedDateTime::class.java,
            Year::class.java, YearMonth::class.java, MonthDay::class.java,
        )

        // 코틀린 표준 라이브러리(kotlin.time), kotlinx-datetime 의 Clock.System.now() 도 시스템 시계를 바로 읽는다
        private val KOTLIN_SYSTEM_CLOCKS = setOf("kotlin.time.Clock\$System", "kotlinx.datetime.Clock\$System")

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

        // JPA 엔티티, 리포지토리, 어댑터는 internal. 엔티티는 어댑터 밖으로 나가지 않고 도메인 모델로 바뀐다
        @ArchTest
        @JvmField
        val outboundAdaptersAreInternal: ArchRule = classes()
            .that().resideInAPackage("..adapter.out..")
            .and().areTopLevelClasses()
            .should(notBeVisibleOutsideModule())
            .because("엔티티, 리포지토리, 어댑터는 포트 인터페이스로만 노출한다")

        // 트랜잭션 경계는 유스케이스 구현(application.service)에만 둔다
        @ArchTest
        @JvmField
        val transactionalClassesOnlyInServices: ArchRule = noClasses()
            .that().resideOutsideOfPackage("..application.service..")
            .should().beAnnotatedWith(SPRING_TRANSACTIONAL)
            .orShould().beAnnotatedWith(JAKARTA_TRANSACTIONAL)
            .because("트랜잭션 경계는 application.service 에 둔다")

        @ArchTest
        @JvmField
        val transactionalMethodsOnlyInServices: ArchRule = noMethods()
            .that().areDeclaredInClassesThat().resideOutsideOfPackage("..application.service..")
            .should().beAnnotatedWith(SPRING_TRANSACTIONAL)
            .orShould().beAnnotatedWith(JAKARTA_TRANSACTIONAL)
            .because("트랜잭션 경계는 application.service 에 둔다")

        // 현재 시각은 Clock Bean 으로 읽는다. Instant.now(clock) 처럼 Clock 을 넘기는 호출은 허용.
        // System.currentTimeMillis() 대신 clock.millis(). 경과 시간 측정용 System.nanoTime() 은 허용
        @ArchTest
        @JvmField
        val noSystemTimeNow: ArchRule = noClasses()
            .should().callMethodWhere(systemTimeNow())
            .orShould().callMethod(System::class.java, "currentTimeMillis")
            .because("현재 시각은 주입받은 Clock 으로 읽어야 테스트에서 고정할 수 있다")

        // 시스템 Clock 은 common.config 의 Clock Bean 에서만 만든다
        @ArchTest
        @JvmField
        val systemClockOnlyInConfig: ArchRule = noClasses()
            .that().resideOutsideOfPackage("$ROOT.common.config..")
            .should().callMethodWhere(systemClockFactory())
            .because("Clock 은 Bean 으로 주입받는다")

        private fun systemTimeNow() = DescribedPredicate.describe<JavaMethodCall>(
            "now() without Clock on java.time types, or Clock.System.now()",
        ) { call ->
            val javaTimeNow = TIME_TYPES.any { call.targetOwner.isEquivalentTo(it) } &&
                call.target.rawParameterTypes.map { it.name } != listOf(Clock::class.java.name)
            call.name == "now" && (javaTimeNow || call.targetOwner.name in KOTLIN_SYSTEM_CLOCKS)
        }

        private fun systemClockFactory() = DescribedPredicate.describe<JavaMethodCall>("Clock.system*()") { call ->
            call.targetOwner.isEquivalentTo(Clock::class.java) && call.name.startsWith("system")
        }

        // 코틀린 클래스는 kotlin-reflect 로 internal/private 인지 본다. 자바 클래스는 public 여부로 본다.
        // 파일 단위 함수가 모인 ...Kt 클래스(@Metadata kind 2)는 클래스 선언이 아니므로 건너뛴다
        private fun notBeVisibleOutsideModule() =
            object : ArchCondition<JavaClass>("be internal or private") {
                override fun check(item: JavaClass, events: ConditionEvents) {
                    val type = item.reflect()
                    val metadata = type.getAnnotation(Metadata::class.java)
                    val visible = when {
                        metadata == null -> item.modifiers.contains(JavaModifier.PUBLIC)
                        metadata.kind != 1 -> return
                        else -> type.kotlin.visibility.let { it == KVisibility.PUBLIC || it == KVisibility.PROTECTED }
                    }
                    if (visible) {
                        events.add(SimpleConditionEvent.violated(item, "${item.name} is visible outside the module"))
                    }
                }
            }

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
