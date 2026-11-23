package com.example.template.item.adapter.`in`.web

import com.example.template.item.domain.Item
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.media.Schema.RequiredMode.REQUIRED
import java.time.Instant

data class ItemResponse(
    @field:Schema(requiredMode = REQUIRED) val id: Long,
    @field:Schema(requiredMode = REQUIRED) val name: String,
    @field:Schema(requiredMode = REQUIRED, types = ["string", "null"]) val description: String?,
    @field:Schema(requiredMode = REQUIRED) val createdAt: Instant,
) {
    companion object {
        // 저장된 Item 만 응답으로 나가므로 id 는 있다
        internal fun from(item: Item) = ItemResponse(item.id!!, item.name, item.description, item.createdAt)
    }
}
