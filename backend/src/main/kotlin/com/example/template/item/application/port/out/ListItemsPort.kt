package com.example.template.item.application.port.out

import com.example.template.item.domain.Item

// fun interface: 메서드가 하나인 인터페이스. 테스트에서 람다로 바로 구현할 수 있다 (자바의 함수형 인터페이스)
fun interface ListItemsPort {
    /** id 내림차순으로 beforeId 보다 작은 항목을 최대 limit 개. beforeId 가 null 이면 처음부터. */
    fun findBefore(beforeId: Long?, limit: Int): List<Item>
}
