package com.example.template.item.application.port.out

import com.example.template.item.application.port.`in`.ItemSearchResult
import com.example.template.item.application.port.`in`.SearchItemsQuery

fun interface SearchItemsPort {
    fun search(query: SearchItemsQuery): ItemSearchResult
}
