package com.example.template.item.application.port.`in`

import com.example.template.common.error.DomainValidationException

/**
 * 자바 record 는 compact constructor 에서 keyword 를 정리(공백 -> null)해서 저장할 수 있지만,
 * 코틀린 data class 는 생성자 프로퍼티를 바꿀 수 없다. 그래서 생성자를 private 으로 막고
 * companion 의 invoke 로 정리한 값을 넘긴다. 호출하는 쪽은 SearchItemsQuery("노트", 0, 20) 로 똑같이 쓴다.
 * @ConsistentCopyVisibility: copy() 도 생성자처럼 private 이 된다 (copy 로 정리 안 된 값을 넣지 못하게)
 */
@ConsistentCopyVisibility
data class SearchItemsQuery private constructor(val keyword: String?, val page: Int, val size: Int) {

    init {
        if (page < 0) {
            throw DomainValidationException("page must be >= 0")
        }
        if (size !in 1..100) {
            throw DomainValidationException("size must be between 1 and 100")
        }
    }

    companion object {
        operator fun invoke(keyword: String?, page: Int, size: Int) = SearchItemsQuery(keyword?.trim()?.ifEmpty { null }, page, size)
    }
}
