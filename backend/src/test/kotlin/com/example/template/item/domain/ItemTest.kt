package com.example.template.item.domain

import com.example.template.common.error.DomainValidationException
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import java.time.Instant

// 자바 버전의 restoreRequiresId 테스트는 없다. restore(id: Long) 은 null 을 받지 않아서 컴파일 단계에서 막힌다
class ItemTest {

    @Test
    fun createTrimsNameAndDescription() {
        val item = Item.create("  노트북  ", "  설명  ", NOW)

        assertThat(item.id).isNull()
        assertThat(item.name).isEqualTo("노트북")
        assertThat(item.description).isEqualTo("설명")
        assertThat(item.createdAt).isEqualTo(NOW)
    }

    @Test
    fun blankDescriptionBecomesNull() {
        assertThat(Item.create("이름", "   ", NOW).description).isNull()
    }

    @Test
    fun rejectsBlankName() {
        assertThatThrownBy { Item.create(" ", null, NOW) }.isInstanceOf(DomainValidationException::class.java)
    }

    @Test
    fun rejectsTooLongName() {
        val name = "a".repeat(Item.NAME_MAX_LENGTH + 1)
        assertThatThrownBy { Item.create(name, null, NOW) }.isInstanceOf(DomainValidationException::class.java)
    }

    @Test
    fun updateKeepsIdAndCreatedAt() {
        val item = Item.restore(1L, "노트북", "업무용", NOW)

        val updated = item.update("  데스크톱 ", null)

        assertThat(updated.id).isEqualTo(1L)
        assertThat(updated.createdAt).isEqualTo(NOW)
        assertThat(updated.name).isEqualTo("데스크톱")
        assertThat(updated.description).isNull()
        assertThat(item.name).isEqualTo("노트북")
    }

    @Test
    fun updateValidatesLikeCreate() {
        val item = Item.restore(1L, "노트북", null, NOW)
        assertThatThrownBy { item.update(" ", null) }.isInstanceOf(DomainValidationException::class.java)
    }

    @Test
    fun unsavedItemCannotBeUpdated() {
        assertThatThrownBy { Item.create("노트북", null, NOW).update("a", null) }
            .isInstanceOf(IllegalStateException::class.java)
    }

    private companion object {
        private val NOW: Instant = Instant.parse("2026-01-01T00:00:00Z")
    }
}
