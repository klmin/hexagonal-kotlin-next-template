package com.example.template.item.application.port.`in`

import com.example.template.item.domain.Item

interface UpdateItemUseCase {
    fun update(command: UpdateItemCommand): Item
}
