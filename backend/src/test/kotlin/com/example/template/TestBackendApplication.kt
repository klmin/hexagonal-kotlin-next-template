package com.example.template

import org.springframework.boot.fromApplication
import org.springframework.boot.with

// ./gradlew bootTestRun (DB를 Testcontainers로 띄워서 실행)
fun main(args: Array<String>) {
    fromApplication<BackendApplication>().with(TestcontainersConfiguration::class).run(*args)
}
