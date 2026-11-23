package com.example.template.common.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import java.time.Clock
import java.time.Duration

@Configuration(proxyBeanMethods = false)
class ClockConfig {

    // PostgreSQL timestamptz 는 마이크로초까지만 저장한다.
    // 나노초까지 쓰면 저장 직후 응답의 시각과 다시 조회한 시각이 달라지므로 마이크로초 단위로 맞춘다
    @Bean
    fun clock(): Clock = Clock.tick(Clock.systemUTC(), Duration.ofNanos(1_000))
}
