package com.example.template.item.application.port.`in`

import com.example.template.common.error.DomainValidationException

/**
 * 커서 기반 목록 조회. 최신순(id 내림차순)으로 cursor 보다 작은 id 부터 size 개를 가져온다.
 *
 * @property cursor 이전 페이지 마지막 항목의 id. 첫 페이지는 null
 */
data class ListItemsQuery(val cursor: Long?, val size: Int) {

    // 자바 record 의 compact constructor 자리. 생성될 때마다 실행된다
    init {
        if (cursor != null && cursor <= 0) {
            throw DomainValidationException("cursor is invalid")
        }
        if (size !in 1..MAX_SIZE) {
            throw DomainValidationException("size must be between 1 and $MAX_SIZE")
        }
    }

    companion object {
        const val MAX_SIZE = 100
    }
}
