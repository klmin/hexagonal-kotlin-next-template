package com.example.template.item.application.port.`in`

import com.example.template.item.domain.Item

interface CreateItemUseCase {
    fun create(command: CreateItemCommand): Item
}
