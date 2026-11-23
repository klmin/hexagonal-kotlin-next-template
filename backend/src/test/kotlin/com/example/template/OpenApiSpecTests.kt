package com.example.template

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.server.LocalServerPort
import org.springframework.context.annotation.Import
import org.springframework.web.client.RestClient
import org.springframework.web.client.body
import org.testcontainers.junit.jupiter.Testcontainers
import tools.jackson.databind.SerializationFeature
import tools.jackson.databind.json.JsonMapper
import java.nio.file.Files
import java.nio.file.Path

// openapi/openapi.json 은 프론트 타입 생성에 쓰는 API 계약 파일이다.
// API를 바꾸면 ./gradlew updateOpenApi 로 갱신하고 같이 커밋한다.
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Import(TestcontainersConfiguration::class)
@Testcontainers(disabledWithoutDocker = true)
class OpenApiSpecTests {

    @LocalServerPort
    private var port: Int = 0

    @Test
    fun committedSpecMatchesApi() {
        // body<String>(): 코틀린 확장 함수. 자바의 body(String.class) 자리
        val body = RestClient.create("http://localhost:$port")
            .get().uri("/v3/api-docs")
            .retrieve().body<String>()
        val actual = MAPPER.readTree(body)

        if (java.lang.Boolean.getBoolean("openapi.update")) {
            Files.createDirectories(SPEC.parent)
            Files.writeString(SPEC, MAPPER.writeValueAsString(actual) + "\n")
            return
        }

        // as 는 코틀린 예약어라 AssertJ 의 as() 는 백틱으로 부른다
        assertThat(Files.exists(SPEC))
            .`as`("%s 가 없습니다. ./gradlew updateOpenApi 를 실행하세요.", SPEC)
            .isTrue()
        val committed = MAPPER.readTree(Files.readString(SPEC))
        assertThat(actual)
            .`as`("API가 openapi/openapi.json 과 다릅니다. ./gradlew updateOpenApi 로 갱신하고 프론트에서 npm run api:types 를 실행하세요.")
            .isEqualTo(committed)
    }

    private companion object {
        private val SPEC: Path = Path.of("..", "openapi", "openapi.json")
        private val MAPPER = JsonMapper.builder().enable(SerializationFeature.INDENT_OUTPUT).build()
    }
}
