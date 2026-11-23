package com.example.template.common.config

import io.micrometer.observation.ObservationPredicate
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.server.observation.ServerRequestObservationContext

@Configuration(proxyBeanMethods = false)
class ObservationConfig {

    // 메트릭 수집(/actuator/prometheus) 요청은 15초마다 오므로 관측하지 않는다.
    // 트레이스뿐 아니라 http.server.requests 메트릭에서도 /actuator 요청이 빠진다
    @Bean
    fun skipActuatorObservations() = ObservationPredicate { _, context ->
        // is 검사 뒤에는 스마트 캐스트로 context 를 ServerRequestObservationContext 로 바로 쓴다 (자바의 instanceof 패턴과 같음)
        // carrier 는 Spring 이 @Nullable 로 표시해 둬서 코틀린은 ?. 없이 쓰지 못하게 막는다 (자바는 경고 없이 통과)
        !(context is ServerRequestObservationContext && context.carrier?.requestURI?.startsWith("/actuator") == true)
    }
}
