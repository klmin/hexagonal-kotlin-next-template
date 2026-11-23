package com.example.template.item.application.service

import com.example.template.item.application.ItemNotFoundException
import com.example.template.item.application.port.`in`.CreateItemCommand
import com.example.template.item.application.port.`in`.CreateItemUseCase
import com.example.template.item.application.port.`in`.DeleteItemUseCase
import com.example.template.item.application.port.`in`.GetItemUseCase
import com.example.template.item.application.port.`in`.UpdateItemCommand
import com.example.template.item.application.port.`in`.UpdateItemUseCase
import com.example.template.item.application.port.out.DeleteItemPort
import com.example.template.item.application.port.out.LoadItemPort
import com.example.template.item.application.port.out.SaveItemPort
import com.example.template.item.application.port.out.UpdateItemPort
import com.example.template.item.domain.Item
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Clock

// 생성자 주입: 주 생성자에 private val 로 받으면 필드 선언과 대입이 한 번에 끝난다
// plugin.spring 이 @Service 클래스를 open 으로 만들어서 @Transactional 프록시가 동작한다
@Service
@Transactional(readOnly = true)
class ItemService(
    private val saveItemPort: SaveItemPort,
    private val loadItemPort: LoadItemPort,
    private val updateItemPort: UpdateItemPort,
    private val deleteItemPort: DeleteItemPort,
    private val clock: Clock,
) : CreateItemUseCase,
    GetItemUseCase,
    UpdateItemUseCase,
    DeleteItemUseCase {

    @Transactional
    override fun create(command: CreateItemCommand): Item {
        val saved = saveItemPort.save(Item.create(command.name, command.description, clock.instant()))
        log.info("item created: id={}", saved.id)
        return saved
    }

    // 조회 -> 도메인 규칙으로 변경 -> 포트로 반영. 반영 방식(JPA 더티 체킹)은 어댑터가 안다
    @Transactional
    override fun update(command: UpdateItemCommand): Item {
        val updated = getItem(command.id).update(command.name, command.description)
        updateItemPort.update(updated)
        log.info("item updated: id={}", updated.id)
        return updated
    }

    @Transactional
    override fun delete(id: Long) {
        if (!deleteItemPort.deleteById(id)) {
            throw ItemNotFoundException(id)
        }
        log.info("item deleted: id={}", id)
    }

    // 엘비스 연산자(?:): 왼쪽이 null 이면 오른쪽을 실행한다. 자바의 Optional.orElseThrow 자리
    override fun getItem(id: Long): Item = loadItemPort.findById(id) ?: throw ItemNotFoundException(id)

    private companion object {
        private val log = LoggerFactory.getLogger(ItemService::class.java)
    }
}
