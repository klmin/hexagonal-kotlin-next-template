package codegen

import org.flywaydb.core.Flyway
import org.jooq.codegen.GenerationTool
import org.jooq.meta.jaxb.Configuration
import org.jooq.meta.jaxb.Database
import org.jooq.meta.jaxb.Generate
import org.jooq.meta.jaxb.Generator
import org.jooq.meta.jaxb.Jdbc
import org.jooq.meta.jaxb.Target
import org.testcontainers.DockerClientFactory
import org.testcontainers.postgresql.PostgreSQLContainer
import org.testcontainers.utility.DockerImageName
import java.nio.file.Path
import kotlin.system.exitProcess

private const val POSTGRES_IMAGE = "postgres:18-alpine"

// ./gradlew generateJooq 에서 실행됨. args: <migration dir> <output dir> <package>
// 최상위 함수 main 은 JooqCodegenKt 클래스의 static main 으로 컴파일된다 (build.gradle.kts 의 mainClass)
fun main(args: Array<String>) {
    val (migrations, output, packageName) = args // 구조 분해: 배열의 앞 세 값을 한 번에 꺼낸다

    if (!DockerClientFactory.instance().isDockerAvailable) {
        System.err.println(
            """
            [generateJooq] Docker에 연결할 수 없습니다.
            jOOQ 코드는 Flyway 마이그레이션을 임시 PostgreSQL 컨테이너에 적용해서 생성합니다.
            Docker를 실행한 뒤 다시 빌드하세요.
            """.trimIndent(),
        )
        exitProcess(1)
    }

    // use: AutoCloseable 을 다 쓰면 닫는다 (자바의 try-with-resources)
    PostgreSQLContainer(DockerImageName.parse(POSTGRES_IMAGE)).use { postgres ->
        postgres.start()

        Flyway.configure()
            .dataSource(postgres.jdbcUrl, postgres.username, postgres.password)
            .locations("filesystem:" + Path.of(migrations).toAbsolutePath())
            .load()
            .migrate()

        GenerationTool.generate(
            Configuration()
                .withJdbc(
                    Jdbc()
                        .withDriver("org.postgresql.Driver")
                        .withUrl(postgres.jdbcUrl)
                        .withUser(postgres.username)
                        .withPassword(postgres.password),
                )
                .withGenerator(
                    Generator()
                        // 코틀린 코드로 생성한다. NOT NULL 컬럼이 코틀린 타입에서도 non-null 이 된다
                        .withName("org.jooq.codegen.KotlinGenerator")
                        .withDatabase(
                            Database()
                                .withName("org.jooq.meta.postgres.PostgresDatabase")
                                .withInputSchema("public")
                                .withExcludes("flyway_schema_history"),
                        )
                        .withGenerate(
                            Generate()
                                .withRecords(true)
                                .withPojos(false)
                                .withDaos(false)
                                .withJavaTimeTypes(true)
                                .withKotlinNotNullRecordAttributes(true),
                        )
                        .withTarget(
                            Target()
                                .withPackageName(packageName)
                                .withDirectory(output)
                                .withClean(true),
                        ),
                ),
        )
    }
}
