package com.example.template.item.application.port.`in`

import com.example.template.item.domain.Item

data class ItemSearchResult(val items: List<Item>, val totalCount: Long, val page: Int, val size: Int)
