package com.example.template.item.adapter.`in`.web

import com.example.template.common.error.DomainValidationException
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test

class ItemCursorTest {

    @Test
    fun roundTrip() {
        val cursor = ItemCursor.encode(42L)
        assertThat(cursor).doesNotContain("42")
        assertThat(ItemCursor.decode(cursor)).isEqualTo(42L)
    }

    @Test
    fun nullAndBlankMeanFirstPage() {
        assertThat(ItemCursor.encode(null)).isNull()
        assertThat(ItemCursor.decode(null)).isNull()
        assertThat(ItemCursor.decode(" ")).isNull()
    }

    @Test
    fun rejectsTamperedCursor() {
        assertThatThrownBy { ItemCursor.decode("not-base64!") }.isInstanceOf(DomainValidationException::class.java)
        assertThatThrownBy { ItemCursor.decode(ItemCursor.encode(-1L)) }.isInstanceOf(DomainValidationException::class.java)
        assertThatThrownBy { ItemCursor.decode("YWJj") }.isInstanceOf(DomainValidationException::class.java)
    }
}
