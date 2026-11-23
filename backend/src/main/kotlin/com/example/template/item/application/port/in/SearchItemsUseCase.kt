package com.example.template.item.application.port.`in`

interface SearchItemsUseCase {
    fun search(query: SearchItemsQuery): ItemSearchResult
}
