package com.example.template.item.application.port.`in`

import com.example.template.common.error.DomainValidationException
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test

class SearchItemsQueryTest {

    @Test
    fun blankKeywordMeansNoFilter() {
        assertThat(SearchItemsQuery("  ", 0, 20).keyword).isNull()
        assertThat(SearchItemsQuery(" 노트 ", 0, 20).keyword).isEqualTo("노트")
    }

    @Test
    fun equalWhenNormalizedValuesAreEqual() {
        // 컨트롤러 테스트에서 every { search(SearchItemsQuery("노트", 0, 20)) } 처럼 값으로 비교한다
        assertThat(SearchItemsQuery(" 노트 ", 0, 20)).isEqualTo(SearchItemsQuery("노트", 0, 20))
    }

    @Test
    fun rejectsInvalidPaging() {
        assertThatThrownBy { SearchItemsQuery(null, -1, 20) }.isInstanceOf(DomainValidationException::class.java)
        assertThatThrownBy { SearchItemsQuery(null, 0, 0) }.isInstanceOf(DomainValidationException::class.java)
        assertThatThrownBy { SearchItemsQuery(null, 0, 101) }.isInstanceOf(DomainValidationException::class.java)
    }
}
