package com.example.template.item.application.service

import com.example.template.item.application.port.`in`.ListItemsQuery
import com.example.template.item.application.port.out.ListItemsPort
import com.example.template.item.application.port.out.SearchItemsPort
import com.example.template.item.domain.Item
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.Instant

class ItemSearchServiceTest {

    // id 1..5 가 저장돼 있는 저장소. ListItemsPort 가 fun interface 라 람다로 바로 만든다
    private val store = ListItemsPort { beforeId, limit ->
        (5L downTo 1L)
            .filter { beforeId == null || it < beforeId }
            .take(limit)
            .map { Item.restore(it, "item-$it", null, NOW) }
    }
    private val service = ItemSearchService(SearchItemsPort { error("검색은 이 테스트에서 쓰지 않는다") }, store)

    @Test
    fun firstPageReturnsNewestAndNextCursor() {
        val result = service.list(ListItemsQuery(null, 2))

        assertThat(result.items.map { it.id }).containsExactly(5L, 4L)
        assertThat(result.nextCursor).isEqualTo(4L)
    }

    @Test
    fun nextPageStartsAfterCursor() {
        val result = service.list(ListItemsQuery(4L, 2))

        assertThat(result.items.map { it.id }).containsExactly(3L, 2L)
        assertThat(result.nextCursor).isEqualTo(2L)
    }

    @Test
    fun lastPageHasNoNextCursor() {
        val result = service.list(ListItemsQuery(2L, 2))

        assertThat(result.items.map { it.id }).containsExactly(1L)
        assertThat(result.nextCursor).isNull()
    }

    @Test
    fun exactlyFullLastPageHasNoNextCursor() {
        val result = service.list(ListItemsQuery(null, 5))

        assertThat(result.items).hasSize(5)
        assertThat(result.nextCursor).isNull()
    }

    @Test
    fun walkingAllPagesVisitsEveryItemOnce() {
        val seen = mutableListOf<Long?>()
        var cursor: Long? = null
        do {
            val page = service.list(ListItemsQuery(cursor, 2))
            page.items.forEach { seen += it.id }
            cursor = page.nextCursor
        } while (cursor != null)

        assertThat(seen).containsExactly(5L, 4L, 3L, 2L, 1L)
    }

    private companion object {
        private val NOW: Instant = Instant.parse("2026-01-01T00:00:00Z")
    }
}
