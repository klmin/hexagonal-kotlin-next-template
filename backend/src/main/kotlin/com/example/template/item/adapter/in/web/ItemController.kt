package com.example.template.item.adapter.`in`.web

import com.example.template.item.application.port.`in`.CreateItemUseCase
import com.example.template.item.application.port.`in`.DeleteItemUseCase
import com.example.template.item.application.port.`in`.GetItemUseCase
import com.example.template.item.application.port.`in`.ListItemsQuery
import com.example.template.item.application.port.`in`.ListItemsUseCase
import com.example.template.item.application.port.`in`.SearchItemsQuery
import com.example.template.item.application.port.`in`.SearchItemsUseCase
import com.example.template.item.application.port.`in`.UpdateItemUseCase
import jakarta.validation.Valid
import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.servlet.support.ServletUriComponentsBuilder

@RestController
@RequestMapping("/api/items")
class ItemController(
    private val createItemUseCase: CreateItemUseCase,
    private val getItemUseCase: GetItemUseCase,
    private val searchItemsUseCase: SearchItemsUseCase,
    private val updateItemUseCase: UpdateItemUseCase,
    private val deleteItemUseCase: DeleteItemUseCase,
    private val listItemsUseCase: ListItemsUseCase,
) {

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    fun create(@Valid @RequestBody request: CreateItemRequest): ResponseEntity<ItemResponse> {
        val item = createItemUseCase.create(request.toCommand())
        val location = ServletUriComponentsBuilder.fromCurrentRequest()
            .path("/{id}")
            .buildAndExpand(item.id)
            .toUri()
        return ResponseEntity.created(location).body(ItemResponse.from(item))
    }

    @GetMapping("/{id}")
    fun get(@PathVariable id: Long): ItemResponse = ItemResponse.from(getItemUseCase.getItem(id))

    @PutMapping("/{id}")
    fun update(@PathVariable id: Long, @Valid @RequestBody request: UpdateItemRequest): ItemResponse = ItemResponse.from(updateItemUseCase.update(request.toCommand(id)))

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun delete(@PathVariable id: Long) {
        deleteItemUseCase.delete(id)
    }

    // 커서 페이징. 첫 페이지는 cursor 없이, 다음 페이지는 응답의 nextCursor 를 그대로 넘긴다
    // String? 이면 Spring 이 선택 파라미터로 본다 (required = false 를 따로 쓰지 않아도 된다)
    // @Max 의 인자는 Long 이라 Int 상수를 toLong() 으로 바꾼다 (자바는 int -> long 자동 변환)
    @GetMapping
    fun list(
        @RequestParam cursor: String?,
        @RequestParam(defaultValue = "20") @Min(1) @Max(ListItemsQuery.MAX_SIZE.toLong()) size: Int,
    ): ItemListResponse = ItemListResponse.from(listItemsUseCase.list(ListItemsQuery(ItemCursor.decode(cursor), size)))

    @GetMapping("/search")
    fun search(
        @RequestParam keyword: String?,
        @RequestParam(defaultValue = "0") @Min(0) page: Int,
        @RequestParam(defaultValue = "20") @Min(1) @Max(100) size: Int,
    ): ItemPageResponse = ItemPageResponse.from(searchItemsUseCase.search(SearchItemsQuery(keyword, page, size)))
}
