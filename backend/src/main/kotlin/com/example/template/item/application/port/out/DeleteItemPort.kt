package com.example.template.item.application.port.out

interface DeleteItemPort {
    /** 삭제했으면 true, 없었으면 false. */
    fun deleteById(id: Long): Boolean
}
