package com.example.template.item.application.service

import com.example.template.item.application.ItemNotFoundException
import com.example.template.item.application.port.`in`.CreateItemCommand
import com.example.template.item.application.port.`in`.UpdateItemCommand
import com.example.template.item.application.port.out.DeleteItemPort
import com.example.template.item.application.port.out.LoadItemPort
import com.example.template.item.application.port.out.SaveItemPort
import com.example.template.item.application.port.out.UpdateItemPort
import com.example.template.item.domain.Item
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset

// port.out 을 인메모리 구현으로 바꿔서 서비스만 테스트한다 (Spring 없이)
class ItemServiceTest {

    private val store = InMemoryItemStore()
    private val service = ItemService(store, store, store, store, Clock.fixed(NOW, ZoneOffset.UTC))

    @Test
    fun createAssignsIdAndCurrentTime() {
        val created = service.create(CreateItemCommand("노트북", "업무용"))

        assertThat(created.id).isEqualTo(1L)
        assertThat(created.createdAt).isEqualTo(NOW)
        assertThat(service.getItem(1L).name).isEqualTo("노트북")
    }

    @Test
    fun getItemThrowsWhenMissing() {
        assertThatThrownBy { service.getItem(99L) }.isInstanceOf(ItemNotFoundException::class.java)
    }

    @Test
    fun updateChangesStoredItem() {
        val id = service.create(CreateItemCommand("노트북", "업무용")).id!!

        val updated = service.update(UpdateItemCommand(id, "데스크톱", null))

        assertThat(updated.name).isEqualTo("데스크톱")
        assertThat(service.getItem(id).name).isEqualTo("데스크톱")
        assertThat(service.getItem(id).createdAt).isEqualTo(NOW)
    }

    @Test
    fun updateThrowsWhenMissing() {
        assertThatThrownBy { service.update(UpdateItemCommand(99L, "a", null)) }
            .isInstanceOf(ItemNotFoundException::class.java)
    }

    @Test
    fun deleteRemovesItem() {
        val id = service.create(CreateItemCommand("노트북", null)).id!!

        service.delete(id)

        assertThatThrownBy { service.getItem(id) }.isInstanceOf(ItemNotFoundException::class.java)
    }

    @Test
    fun deleteThrowsWhenMissing() {
        assertThatThrownBy { service.delete(99L) }.isInstanceOf(ItemNotFoundException::class.java)
    }

    private class InMemoryItemStore : SaveItemPort, LoadItemPort, UpdateItemPort, DeleteItemPort {
        private val items = mutableListOf<Item>()

        // 실제 DB 시퀀스처럼 지워도 번호를 재사용하지 않는다 (items.size + 1 은 삭제 뒤 중복 id 를 만든다)
        private var sequence = 0L

        override fun save(item: Item): Item =
            Item.restore(++sequence, item.name, item.description, item.createdAt).also { items += it }

        override fun findById(id: Long): Item? = items.find { it.id == id }

        override fun update(item: Item) {
            items.replaceAll { if (it.id == item.id) item else it }
        }

        override fun deleteById(id: Long): Boolean = items.removeIf { it.id == id }
    }

    private companion object {
        private val NOW: Instant = Instant.parse("2026-01-01T00:00:00Z")
    }
}
