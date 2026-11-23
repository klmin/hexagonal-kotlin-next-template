package com.example.template.item.application.port.`in`

// 자바 record 자리는 data class. equals/hashCode/toString 이 생긴다 (테스트에서 같은 값인지 비교할 때 필요)
data class CreateItemCommand(val name: String, val description: String?)
