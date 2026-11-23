package com.example.template.item.adapter.out.persistence

import com.example.template.item.application.port.out.DeleteItemPort
import com.example.template.item.application.port.out.LoadItemPort
import com.example.template.item.application.port.out.SaveItemPort
import com.example.template.item.application.port.out.UpdateItemPort
import com.example.template.item.domain.Item
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Component

@Component
internal class ItemPersistenceAdapter(
    private val repository: ItemJpaRepository,
) : SaveItemPort,
    LoadItemPort,
    UpdateItemPort,
    DeleteItemPort {

    override fun save(item: Item): Item = repository.save(item.toEntity()).toDomain()

    // 영속 상태 엔티티에 값만 옮긴다. save() 없이 트랜잭션 커밋 때 더티 체킹으로 UPDATE 된다.
    // 같은 트랜잭션에서 방금 조회한 엔티티는 1차 캐시에서 나오므로 SELECT가 다시 나가지 않는다
    override fun update(item: Item) {
        val id = checkNotNull(item.id) { "저장되지 않은 Item" }
        val entity = repository.findByIdOrNull(id) ?: throw IllegalStateException("item not found: $id")
        entity.update(item.name, item.description)
    }

    override fun deleteById(id: Long): Boolean {
        if (!repository.existsById(id)) {
            return false
        }
        repository.deleteById(id)
        return true
    }

    // findByIdOrNull: Spring Data 의 코틀린 확장 함수. Optional 대신 null 을 돌려준다
    override fun findById(id: Long): Item? = repository.findByIdOrNull(id)?.toDomain()

    // 변환은 확장 함수로. 이 파일 밖에서는 안 보인다(private)
    private fun Item.toEntity() = ItemJpaEntity(name, description, createdAt)

    private fun ItemJpaEntity.toDomain() = Item.restore(id!!, name, description, createdAt)
}
