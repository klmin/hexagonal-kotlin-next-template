package com.example.template.item.application.port.`in`

import com.example.template.item.domain.Item

/**
 * @property nextCursor 다음 페이지를 가져올 커서. 마지막 페이지면 null
 */
data class ItemListResult(val items: List<Item>, val nextCursor: Long?)
