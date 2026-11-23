package com.example.template.common.error

// 도메인 규칙 위반 -> 400 (GlobalExceptionHandler)
class DomainValidationException(message: String) : RuntimeException(message)
