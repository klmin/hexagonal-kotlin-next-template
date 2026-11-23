package com.example.template.item.adapter.`in`.web

import com.example.template.item.application.port.`in`.ItemListResult
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.media.Schema.RequiredMode.REQUIRED

data class ItemListResponse(
    @field:Schema(requiredMode = REQUIRED) val items: List<ItemResponse>,
    @field:Schema(
        requiredMode = REQUIRED,
        types = ["string", "null"],
        description = "다음 페이지 커서. 마지막 페이지면 null",
    ) val nextCursor: String?,
) {
    companion object {
        internal fun from(result: ItemListResult) = ItemListResponse(
            result.items.map(ItemResponse::from),
            ItemCursor.encode(result.nextCursor),
        )
    }
}
