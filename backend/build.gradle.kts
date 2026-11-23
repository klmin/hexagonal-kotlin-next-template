plugins {
    // 버전은 Spring Boot 가 관리하는 kotlin.version 과 맞춘다
    kotlin("jvm") version "2.4.21"
    // @Component, @Service, @Transactional, @Configuration 등이 붙은 클래스를 open 으로 만든다 (코틀린 클래스는 기본 final -> 프록시 불가)
    kotlin("plugin.spring") version "2.4.21"
    // @Entity 등에 기본 생성자를 만들어 준다 (JPA 가 리플렉션으로 생성할 때 필요)
    kotlin("plugin.jpa") version "2.4.21"
    id("org.springframework.boot") version "4.2.0"
    id("io.spring.dependency-management") version "1.1.7"
}

group = "com.example"
version = "0.0.1-SNAPSHOT"

kotlin {
    jvmToolchain(25)
    compilerOptions {
        // Spring 7 은 JSpecify(@Nullable)를 쓰고 코틀린 2.x 는 이를 기본으로 엄격하게 반영한다.
        // -Xjsr305=strict 는 아직 JSR-305(javax.annotation) 를 쓰는 라이브러리까지 같은 방식으로 맞춘다.
        // (코틀린 2.4 부터 생성자 프로퍼티의 어노테이션은 기본으로 파라미터와 필드 모두에 붙는다. docs/kotlin-vs-java.md 3번)
        freeCompilerArgs.addAll("-Xjsr305=strict")
        // 경고를 오류로. 지금 경고가 0개라 비용이 없고, null 관련 경고가 쌓이지 않게 막는다
        allWarningsAsErrors = true
        // 파라미터 이름을 바이트코드에 남긴다. 검증 오류의 field("size")와 @RequestParam 이름 추론에 필요
        javaParameters = true
    }
}

// JPA 엔티티는 지연 로딩 프록시를 만들 수 있게 open 으로 (plugin.spring 은 @Entity 를 열지 않는다)
allOpen {
    annotation("jakarta.persistence.Entity")
    annotation("jakarta.persistence.MappedSuperclass")
    annotation("jakarta.persistence.Embeddable")
}

repositories {
    mavenCentral()
}

// jOOQ 코드 생성 전용 소스셋 (src/jooqCodegen). 앱에는 포함되지 않는다.
val jooqCodegen = sourceSets.create("jooqCodegen")

dependencies {
    implementation("org.springframework.boot:spring-boot-starter-actuator")
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    implementation("org.springframework.boot:spring-boot-starter-jooq")
    implementation("org.springframework.boot:spring-boot-starter-opentelemetry")
    implementation("org.springframework.boot:spring-boot-starter-flyway")
    implementation("org.springframework.boot:spring-boot-starter-validation")
    implementation("org.springframework.boot:spring-boot-starter-webmvc")
    implementation("org.flywaydb:flyway-database-postgresql")
    implementation("org.springdoc:springdoc-openapi-starter-webmvc-ui:3.2.1")
    implementation("org.jetbrains.kotlin:kotlin-reflect")
    // 코틀린 data class 를 JSON 으로 바꾸고 읽는다 (Jackson 3). 기본값, nullable 을 코틀린 방식대로 처리
    implementation("tools.jackson.module:jackson-module-kotlin")
    developmentOnly("org.springframework.boot:spring-boot-docker-compose")
    runtimeOnly("org.postgresql:postgresql")
    runtimeOnly("io.micrometer:micrometer-registry-prometheus")

    testImplementation("org.springframework.boot:spring-boot-starter-actuator-test")
    testImplementation("org.springframework.boot:spring-boot-starter-data-jpa-test")
    testImplementation("org.springframework.boot:spring-boot-starter-flyway-test")
    testImplementation("org.springframework.boot:spring-boot-starter-jooq-test")
    testImplementation("org.springframework.boot:spring-boot-starter-webmvc-test")
    testImplementation("org.springframework.boot:spring-boot-testcontainers")
    testImplementation("org.testcontainers:testcontainers-junit-jupiter")
    testImplementation("org.testcontainers:testcontainers-postgresql")
    testImplementation("com.tngtech.archunit:archunit-junit5:1.5.1")
    // 코틀린용 목 라이브러리. @MockitoBean 대신 @MockkBean
    testImplementation("io.mockk:mockk:1.14.12")
    testImplementation("com.ninja-squad:springmockk:5.0.1")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")

    "jooqCodegenImplementation"("org.jooq:jooq-codegen")
    "jooqCodegenImplementation"("org.flywaydb:flyway-core")
    "jooqCodegenImplementation"("org.flywaydb:flyway-database-postgresql")
    "jooqCodegenImplementation"("org.testcontainers:testcontainers-postgresql")
    "jooqCodegenRuntimeOnly"("org.postgresql:postgresql")
    "jooqCodegenRuntimeOnly"("org.slf4j:slf4j-simple")
}

// Flyway 마이그레이션 -> 임시 PostgreSQL(Testcontainers) -> jOOQ 코드 생성
// 마이그레이션 파일이 바뀔 때만 다시 실행된다.
val migrationDir = layout.projectDirectory.dir("src/main/resources/db/migration")
val jooqOutputDir = layout.buildDirectory.dir("generated-sources/jooq")
val jooqPackage = "com.example.template.common.jooq"

val generateJooq = tasks.register<JavaExec>("generateJooq") {
    group = "build"
    description = "Flyway 마이그레이션으로 jOOQ 코드를 생성한다 (Docker 필요)"
    classpath = jooqCodegen.runtimeClasspath
    // 코틀린 최상위 main 함수는 <파일이름>Kt 클래스로 컴파일된다
    mainClass = "codegen.JooqCodegenKt"
    // 안내 메시지(한글)가 Windows 기본 인코딩(MS949)으로 나와 Gradle 출력에서 깨지지 않게
    jvmArgs("-Dstdout.encoding=UTF-8", "-Dstderr.encoding=UTF-8")
    inputs.dir(migrationDir).withPathSensitivity(PathSensitivity.RELATIVE)
    inputs.files(jooqCodegen.runtimeClasspath).withNormalizer(ClasspathNormalizer::class)
    // 패키지를 바꾸면(scripts/init-project.mjs) 다시 생성되게 입력으로 등록한다
    inputs.property("jooqPackage", jooqPackage)
    outputs.dir(jooqOutputDir)
    outputs.cacheIf { true }
    argumentProviders.add(CommandLineArgumentProvider {
        listOf(
            migrationDir.asFile.absolutePath,
            jooqOutputDir.get().asFile.absolutePath,
            jooqPackage,
        )
    })
    // 이전 패키지로 생성된 코드가 남아 같이 컴파일되지 않게 비우고 생성한다
    doFirst {
        jooqOutputDir.get().asFile.deleteRecursively()
    }
}

// 생성 코드도 코틀린(KotlinGenerator)이라 kotlin 소스로 등록한다
kotlin.sourceSets.main {
    kotlin.srcDir(generateJooq.map { jooqOutputDir })
}

tasks.withType<Test> {
    useJUnitPlatform()
    testLogging {
        // Docker가 없어 skip된 테스트를 CI 로그에서 바로 보이게
        events("skipped", "failed")
        exceptionFormat = org.gradle.api.tasks.testing.logging.TestExceptionFormat.FULL
    }
    addTestListener(object : TestListener {
        override fun beforeSuite(suite: TestDescriptor) {}
        override fun beforeTest(testDescriptor: TestDescriptor) {}
        override fun afterTest(testDescriptor: TestDescriptor, result: TestResult) {}
        override fun afterSuite(suite: TestDescriptor, result: TestResult) {
            if (suite.parent == null) {
                println(
                    "Tests: ${result.testCount}, passed: ${result.successfulTestCount}, " +
                        "failed: ${result.failedTestCount}, skipped: ${result.skippedTestCount}"
                )
            }
        }
    })
}

// openapi/openapi.json(API 계약)을 현재 코드 기준으로 다시 쓴다 (Docker 필요)
tasks.register<Test>("updateOpenApi") {
    group = "documentation"
    description = "openapi/openapi.json 을 현재 API로 갱신한다"
    testClassesDirs = sourceSets.test.get().output.classesDirs
    classpath = sourceSets.test.get().runtimeClasspath
    filter.includeTestsMatching("*OpenApiSpecTests")
    systemProperty("openapi.update", "true")
    outputs.upToDateWhen { false }
    // Docker가 없으면 테스트가 skip되어 스펙을 갱신하지 못한 채 성공으로 끝나므로 실패시킨다
    var updated = false
    addTestListener(object : TestListener {
        override fun beforeSuite(suite: TestDescriptor) {}
        override fun beforeTest(testDescriptor: TestDescriptor) {}
        override fun afterTest(testDescriptor: TestDescriptor, result: TestResult) {}
        override fun afterSuite(suite: TestDescriptor, result: TestResult) {
            if (suite.parent == null) {
                updated = result.successfulTestCount > 0
            }
        }
    })
    doLast {
        if (!updated) {
            throw GradleException("openapi/openapi.json 을 갱신하지 못했습니다. Docker를 실행한 뒤 다시 실행하세요.")
        }
    }
}

// Docker 이미지에는 실행 jar(bootJar) 하나만 쓴다
tasks.jar {
    enabled = false
}
