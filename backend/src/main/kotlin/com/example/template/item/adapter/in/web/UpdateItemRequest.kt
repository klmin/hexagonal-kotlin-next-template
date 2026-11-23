package com.example.template.item.adapter.`in`.web

import com.example.template.item.application.port.`in`.UpdateItemCommand
import com.example.template.item.domain.Item
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size

data class UpdateItemRequest(
    @field:NotBlank @field:Size(max = Item.NAME_MAX_LENGTH) val name: String?,
    @field:Size(max = Item.DESCRIPTION_MAX_LENGTH) val description: String? = null,
) {
    internal fun toCommand(id: Long) = UpdateItemCommand(id, name!!, description)
}
