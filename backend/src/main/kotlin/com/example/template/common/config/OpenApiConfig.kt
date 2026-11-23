package com.example.template.common.config

import io.swagger.v3.oas.models.OpenAPI
import io.swagger.v3.oas.models.info.Info
import io.swagger.v3.oas.models.servers.Server
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration(proxyBeanMethods = false)
class OpenApiConfig {

    @Bean
    fun openApi(): OpenAPI = OpenAPI()
        .info(Info().title("Backend API").version("v1"))
        // 실행 포트가 스펙에 들어가지 않게 고정 (openapi/openapi.json 비교용)
        .servers(listOf(Server().url("/")))
}
