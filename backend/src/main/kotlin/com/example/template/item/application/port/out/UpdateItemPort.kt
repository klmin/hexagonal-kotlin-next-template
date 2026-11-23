package com.example.template.item.application.port.out

import com.example.template.item.domain.Item

interface UpdateItemPort {
    /** 이미 저장된 Item 의 변경 내용을 반영한다. */
    fun update(item: Item)
}
