package com.example.template.item.application.port.`in`

import com.example.template.common.error.DomainValidationException
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test

class ListItemsQueryTest {

    @Test
    fun rejectsInvalidValues() {
        assertThatThrownBy { ListItemsQuery(0L, 20) }.isInstanceOf(DomainValidationException::class.java)
        assertThatThrownBy { ListItemsQuery(null, 0) }.isInstanceOf(DomainValidationException::class.java)
        assertThatThrownBy { ListItemsQuery(null, ListItemsQuery.MAX_SIZE + 1) }
            .isInstanceOf(DomainValidationException::class.java)
    }
}
