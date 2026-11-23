package com.example.template.item.application.port.`in`

import com.example.template.item.domain.Item

interface GetItemUseCase {
    fun getItem(id: Long): Item
}
