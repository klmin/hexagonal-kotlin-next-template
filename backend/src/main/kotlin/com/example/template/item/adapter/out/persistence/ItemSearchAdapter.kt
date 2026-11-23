package com.example.template.item.adapter.out.persistence

import com.example.template.common.jooq.tables.records.ItemsRecord
import com.example.template.common.jooq.tables.references.ITEMS
import com.example.template.item.application.port.out.ItemPageResult
import com.example.template.item.application.port.out.ListItemsPort
import com.example.template.item.application.port.out.SearchItemsPort
import com.example.template.item.domain.Item
import org.jooq.DSLContext
import org.jooq.impl.DSL.noCondition
import org.springframework.stereotype.Component

// 조회 전용. 저장/수정은 JPA(ItemPersistenceAdapter), 검색 같은 조회는 jOOQ
@Component
internal class ItemSearchAdapter(
    private val dsl: DSLContext,
) : SearchItemsPort,
    ListItemsPort {

    // 커서(키셋) 페이징: OFFSET 없이 PK 인덱스로 바로 찾아가서 뒤 페이지도 빠르다
    override fun findBefore(beforeId: Long?, limit: Int): List<Item> {
        val condition = if (beforeId == null) noCondition() else ITEMS.ID.lt(beforeId)
        return dsl.selectFrom(ITEMS)
            .where(condition)
            .orderBy(ITEMS.ID.desc())
            .limit(limit)
            .fetch { it.toDomain() }
    }

    override fun search(keyword: String?, offset: Long, limit: Int): ItemPageResult {
        // let: keyword 가 null 이 아닐 때만 조건을 만든다
        val condition = keyword?.let { ITEMS.NAME.containsIgnoreCase(it) } ?: noCondition()
        val items = dsl.selectFrom(ITEMS)
            .where(condition)
            .orderBy(ITEMS.ID.desc())
            .limit(limit)
            .offset(offset)
            .fetch { it.toDomain() }
        val total = dsl.fetchCount(ITEMS, condition).toLong()
        return ItemPageResult(items, total)
    }

    // name, createdAt 은 NOT NULL 컬럼이라 생성 코드에서도 non-null. id 는 DB 가 채우는 값이라 nullable 로 생성된다
    private fun ItemsRecord.toDomain() = Item.restore(id!!, name, description, createdAt.toInstant())
}
