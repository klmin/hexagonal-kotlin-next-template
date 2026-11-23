package com.example.template.item.application.port.`in`

interface ListItemsUseCase {
    fun list(query: ListItemsQuery): ItemListResult
}
