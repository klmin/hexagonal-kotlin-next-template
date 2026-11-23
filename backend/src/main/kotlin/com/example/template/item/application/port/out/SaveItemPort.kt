package com.example.template.item.application.port.out

import com.example.template.item.domain.Item

interface SaveItemPort {
    fun save(item: Item): Item
}
