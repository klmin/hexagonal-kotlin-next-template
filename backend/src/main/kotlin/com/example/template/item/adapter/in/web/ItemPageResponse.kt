package com.example.template.item.adapter.`in`.web

import com.example.template.item.application.port.`in`.ItemSearchResult
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.media.Schema.RequiredMode.REQUIRED

data class ItemPageResponse(
    @field:Schema(requiredMode = REQUIRED) val items: List<ItemResponse>,
    @field:Schema(requiredMode = REQUIRED) val totalCount: Long,
    @field:Schema(requiredMode = REQUIRED) val page: Int,
    @field:Schema(requiredMode = REQUIRED) val size: Int,
) {
    companion object {
        internal fun from(result: ItemSearchResult) = ItemPageResponse(
            result.items.map(ItemResponse::from),
            result.totalCount,
            result.page,
            result.size,
        )
    }
}
