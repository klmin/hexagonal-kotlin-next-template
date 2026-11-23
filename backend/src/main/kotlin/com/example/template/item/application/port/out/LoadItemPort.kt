package com.example.template.item.application.port.out

import com.example.template.item.domain.Item

interface LoadItemPort {
    // 자바는 Optional<Item>, 코틀린은 nullable 타입(Item?)으로 "없을 수 있음"을 표현한다
    fun findById(id: Long): Item?
}
