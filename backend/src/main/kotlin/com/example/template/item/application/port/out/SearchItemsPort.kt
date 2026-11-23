package com.example.template.item.application.port.out

// port.in 의 Query/Result 를 받지 않는다. 출력 포트는 저장소가 알아야 할 값만 받는다
fun interface SearchItemsPort {
    // keyword 가 null 이면 전체. 최신순(id 내림차순)
    fun search(keyword: String?, offset: Long, limit: Int): ItemPageResult
}
