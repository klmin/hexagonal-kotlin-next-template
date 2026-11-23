package com.example.template

import io.micrometer.core.instrument.MeterRegistry
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.micrometer.metrics.test.autoconfigure.AutoConfigureMetrics
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.server.LocalManagementPort
import org.springframework.boot.test.web.server.LocalServerPort
import org.springframework.context.ApplicationContext
import org.springframework.context.annotation.Import
import org.springframework.web.client.RestClient
import org.springframework.web.client.body
import org.testcontainers.junit.jupiter.Testcontainers

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = ["management.server.port=0"])
@AutoConfigureMetrics
@Import(TestcontainersConfiguration::class)
@Testcontainers(disabledWithoutDocker = true)
class ObservabilityTests {

    @LocalServerPort
    private var port: Int = 0

    @LocalManagementPort
    private var managementPort: Int = 0

    // lateinit: 만들 때는 비어 있고 Spring 이 나중에 넣어 준다. 넣기 전에 읽으면 예외
    @Autowired
    private lateinit var context: ApplicationContext

    @Test
    fun exposesPrometheusMetricsOnManagementPort() {
        RestClient.create("http://localhost:$port").get().uri("/api/items").retrieve().toBodilessEntity()

        val metrics = RestClient.create("http://localhost:$managementPort")
            .get().uri("/actuator/prometheus")
            .retrieve().body<String>()

        assertThat(metrics)
            .contains("http_server_requests_seconds_bucket")
            .contains("uri=\"/api/items\"")
            .contains("application=\"backend\"")
            .contains("hikaricp_connections_active")
    }

    // 메트릭은 Alloy가 /actuator/prometheus 를 가져간다(pull). starter-opentelemetry 에 들어 있는
    // OTLP 메트릭 전송기가 켜지면 수집기가 없는 곳에서 1분마다 전송 실패 로그가 남으므로 꺼져 있어야 한다
    @Test
    fun doesNotPushMetricsOverOtlp() {
        assertThat(context.getBeansOfType(MeterRegistry::class.java).values.map { it.javaClass.simpleName })
            .contains("PrometheusMeterRegistry")
            .doesNotContain("OtlpMeterRegistry")
    }
}
