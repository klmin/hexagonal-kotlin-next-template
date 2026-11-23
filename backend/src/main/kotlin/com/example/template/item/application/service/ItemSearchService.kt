package com.example.template.item.application.service

import com.example.template.item.application.port.`in`.ItemListResult
import com.example.template.item.application.port.`in`.ItemSearchResult
import com.example.template.item.application.port.`in`.ListItemsQuery
import com.example.template.item.application.port.`in`.ListItemsUseCase
import com.example.template.item.application.port.`in`.SearchItemsQuery
import com.example.template.item.application.port.`in`.SearchItemsUseCase
import com.example.template.item.application.port.out.ListItemsPort
import com.example.template.item.application.port.out.SearchItemsPort
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional(readOnly = true)
class ItemSearchService(
    private val searchItemsPort: SearchItemsPort,
    private val listItemsPort: ListItemsPort,
) : SearchItemsUseCase, ListItemsUseCase {

    // size + 1 개를 읽어서 다음 페이지가 있는지 판단한다 (count 쿼리 없음)
    override fun list(query: ListItemsQuery): ItemListResult {
        val rows = listItemsPort.findBefore(query.cursor, query.size + 1)
        if (rows.size <= query.size) {
            return ItemListResult(rows, null)
        }
        val page = rows.take(query.size)
        return ItemListResult(page, page.last().id)
    }

    override fun search(query: SearchItemsQuery): ItemSearchResult = searchItemsPort.search(query)
}
