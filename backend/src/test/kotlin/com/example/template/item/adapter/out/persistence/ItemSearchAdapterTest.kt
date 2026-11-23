package com.example.template.item.adapter.out.persistence

import com.example.template.TestcontainersConfiguration
import com.example.template.common.jooq.tables.references.ITEMS
import com.example.template.item.application.port.`in`.SearchItemsQuery
import org.assertj.core.api.Assertions.assertThat
import org.jooq.DSLContext
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.jooq.test.autoconfigure.JooqTest
import org.springframework.context.annotation.Import
import org.testcontainers.junit.jupiter.Testcontainers
import java.time.OffsetDateTime
import java.time.ZoneOffset

@JooqTest
@Import(TestcontainersConfiguration::class, ItemSearchAdapter::class)
@Testcontainers(disabledWithoutDocker = true)
class ItemSearchAdapterTest {

    @Autowired
    private lateinit var dsl: DSLContext

    @Autowired
    private lateinit var adapter: ItemSearchAdapter

    @BeforeEach
    fun setUp() {
        listOf("노트북", "노트", "마우스", "Notebook Stand").forEach { name ->
            dsl.insertInto(ITEMS)
                .set(ITEMS.NAME, name)
                .set(ITEMS.CREATED_AT, NOW)
                .execute()
        }
    }

    @Test
    fun searchesByKeywordIgnoringCase() {
        val result = adapter.search(SearchItemsQuery("NOTE", 0, 20))

        assertThat(result.items.map { it.name }).containsExactly("Notebook Stand")
        assertThat(result.totalCount).isEqualTo(1)
    }

    @Test
    fun pagesNewestFirst() {
        val first = adapter.search(SearchItemsQuery(null, 0, 3))
        val second = adapter.search(SearchItemsQuery(null, 1, 3))

        assertThat(first.totalCount).isEqualTo(4)
        assertThat(first.items.map { it.name }).containsExactly("Notebook Stand", "마우스", "노트")
        assertThat(second.items.map { it.name }).containsExactly("노트북")
    }

    @Test
    fun findBeforeReturnsNewestFirstAfterCursor() {
        val first = adapter.findBefore(null, 2)
        val next = adapter.findBefore(first.last().id, 10)

        assertThat(first.map { it.name }).containsExactly("Notebook Stand", "마우스")
        assertThat(next.map { it.name }).containsExactly("노트", "노트북")
    }

    private companion object {
        private val NOW: OffsetDateTime = OffsetDateTime.of(2026, 1, 1, 0, 0, 0, 0, ZoneOffset.UTC)
    }
}
