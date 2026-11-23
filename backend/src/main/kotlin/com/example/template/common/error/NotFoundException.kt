package com.example.template.common.error

// 없는 리소스 -> 404. 기능마다 상속해서 쓴다 (예: ItemNotFoundException)
abstract class NotFoundException(message: String) : RuntimeException(message)
