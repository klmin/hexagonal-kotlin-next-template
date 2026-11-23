package com.example.template.item.adapter.`in`.web

import com.example.template.common.error.DomainValidationException
import com.example.template.item.application.ItemNotFoundException
import com.example.template.item.application.port.`in`.CreateItemUseCase
import com.example.template.item.application.port.`in`.DeleteItemUseCase
import com.example.template.item.application.port.`in`.GetItemUseCase
import com.example.template.item.application.port.`in`.ItemListResult
import com.example.template.item.application.port.`in`.ItemSearchResult
import com.example.template.item.application.port.`in`.ListItemsQuery
import com.example.template.item.application.port.`in`.ListItemsUseCase
import com.example.template.item.application.port.`in`.SearchItemsQuery
import com.example.template.item.application.port.`in`.SearchItemsUseCase
import com.example.template.item.application.port.`in`.UpdateItemCommand
import com.example.template.item.application.port.`in`.UpdateItemUseCase
import com.example.template.item.domain.Item
import com.ninjasquad.springmockk.MockkBean
import io.mockk.every
import io.mockk.justRun
import io.mockk.verify
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.delete
import org.springframework.test.web.servlet.get
import org.springframework.test.web.servlet.post
import org.springframework.test.web.servlet.put
import java.time.Instant

/**
 * 자바 버전과의 차이
 * - @MockitoBean 대신 springmockk 의 @MockkBean. given(...).willReturn(...) 대신 every { ... } returns ...
 * - MockMvc 코틀린 DSL: mockMvc.post("/url") { content = ... }.andExpect { status { isCreated() } }
 * - 여러 줄 JSON 은 원시 문자열(""" ... """)
 */
@WebMvcTest(ItemController::class)
class ItemControllerTest {

    @Autowired
    private lateinit var mockMvc: MockMvc

    @MockkBean
    private lateinit var createItemUseCase: CreateItemUseCase

    @MockkBean
    private lateinit var getItemUseCase: GetItemUseCase

    @MockkBean
    private lateinit var searchItemsUseCase: SearchItemsUseCase

    @MockkBean
    private lateinit var updateItemUseCase: UpdateItemUseCase

    @MockkBean
    private lateinit var deleteItemUseCase: DeleteItemUseCase

    @MockkBean
    private lateinit var listItemsUseCase: ListItemsUseCase

    @Test
    fun createReturns201WithLocation() {
        every { createItemUseCase.create(any()) } returns Item.restore(1L, "노트북", null, NOW)

        mockMvc.post("/api/items") {
            contentType = MediaType.APPLICATION_JSON
            content = """{"name": "노트북"}"""
        }.andExpect {
            status { isCreated() }
            header { string("Location", "http://localhost/api/items/1") }
            jsonPath("$.id") { value(1) }
            jsonPath("$.name") { value("노트북") }
        }
    }

    @Test
    fun createRejectsBlankName() {
        mockMvc.post("/api/items") {
            contentType = MediaType.APPLICATION_JSON
            content = """{"name": ""}"""
        }.andExpect {
            status { isBadRequest() }
            header { string("Content-Type", MediaType.APPLICATION_PROBLEM_JSON_VALUE) }
            jsonPath("$.errors[0].field") { value("name") }
        }
    }

    @Test
    fun createRejectsMissingName() {
        // name 이 String? 라 Jackson 단계가 아니라 Bean Validation 에서 걸린다 (CreateItemRequest 주석 참고)
        mockMvc.post("/api/items") {
            contentType = MediaType.APPLICATION_JSON
            content = """{}"""
        }.andExpect {
            status { isBadRequest() }
            jsonPath("$.errors[0].field") { value("name") }
        }
    }

    @Test
    fun getReturns404WhenMissing() {
        every { getItemUseCase.getItem(99L) } throws ItemNotFoundException(99L)

        mockMvc.get("/api/items/99").andExpect {
            status { isNotFound() }
            jsonPath("$.detail") { value("item not found: 99") }
        }
    }

    @Test
    fun listReturnsFirstPageWithOpaqueNextCursor() {
        every { listItemsUseCase.list(ListItemsQuery(null, 20)) } returns
            ItemListResult(listOf(Item.restore(7L, "a", null, NOW)), 7L)

        mockMvc.get("/api/items").andExpect {
            status { isOk() }
            jsonPath("$.items[0].name") { value("a") }
            jsonPath("$.nextCursor") { value(ItemCursor.encode(7L)) }
        }
    }

    @Test
    fun listPassesDecodedCursor() {
        every { listItemsUseCase.list(ListItemsQuery(7L, 2)) } returns ItemListResult(emptyList(), null)

        mockMvc.get("/api/items") {
            param("cursor", ItemCursor.encode(7L)!!)
            param("size", "2")
        }.andExpect {
            status { isOk() }
            jsonPath("$.nextCursor") { doesNotExist() }
        }
    }

    @Test
    fun listRejectsInvalidCursorAndSize() {
        mockMvc.get("/api/items") { param("cursor", "not-a-cursor!") }
            .andExpect { status { isBadRequest() } }
        mockMvc.get("/api/items") { param("size", "101") }
            .andExpect { status { isBadRequest() } }
    }

    @Test
    fun searchReturnsPage() {
        every { searchItemsUseCase.search(SearchItemsQuery("노트", 0, 20)) } returns
            ItemSearchResult(listOf(Item.restore(1L, "노트북", null, NOW)), 1, 0, 20)

        mockMvc.get("/api/items/search") { param("keyword", "노트") }.andExpect {
            status { isOk() }
            jsonPath("$.items[0].name") { value("노트북") }
            jsonPath("$.totalCount") { value(1) }
        }
    }

    @Test
    fun searchRejectsTooLargePageSize() {
        mockMvc.get("/api/items/search") { param("size", "101") }.andExpect {
            status { isBadRequest() }
            header { string("Content-Type", MediaType.APPLICATION_PROBLEM_JSON_VALUE) }
            jsonPath("$.detail") { value("입력값을 확인해 주세요.") }
            jsonPath("$.errors[0].field") { value("size") }
        }
    }

    @Test
    fun createReturns400WhenDomainRuleViolated() {
        every { createItemUseCase.create(any()) } throws DomainValidationException("이미 있는 이름입니다.")

        mockMvc.post("/api/items") {
            contentType = MediaType.APPLICATION_JSON
            content = """{"name": "노트북"}"""
        }.andExpect {
            status { isBadRequest() }
            header { string("Content-Type", MediaType.APPLICATION_PROBLEM_JSON_VALUE) }
            jsonPath("$.detail") { value("이미 있는 이름입니다.") }
        }
    }

    @Test
    fun createRejectsMalformedJson() {
        mockMvc.post("/api/items") {
            contentType = MediaType.APPLICATION_JSON
            content = """{"name": """
        }.andExpect {
            status { isBadRequest() }
            header { string("Content-Type", MediaType.APPLICATION_PROBLEM_JSON_VALUE) }
        }
    }

    @Test
    fun getRejectsNonNumericId() {
        mockMvc.get("/api/items/abc").andExpect {
            status { isBadRequest() }
            header { string("Content-Type", MediaType.APPLICATION_PROBLEM_JSON_VALUE) }
        }
    }

    @Test
    fun unexpectedErrorReturns500ProblemWithoutInternalMessage() {
        every { listItemsUseCase.list(any()) } throws IllegalStateException("db password=secret")

        mockMvc.get("/api/items").andExpect {
            status { isInternalServerError() }
            header { string("Content-Type", MediaType.APPLICATION_PROBLEM_JSON_VALUE) }
            jsonPath("$.detail") { value("서버 오류가 발생했습니다.") }
        }
    }

    @Test
    fun updateReturnsUpdatedItem() {
        every { updateItemUseCase.update(UpdateItemCommand(1L, "데스크톱", null)) } returns
            Item.restore(1L, "데스크톱", null, NOW)

        mockMvc.put("/api/items/1") {
            contentType = MediaType.APPLICATION_JSON
            content = """{"name": "데스크톱"}"""
        }.andExpect {
            status { isOk() }
            jsonPath("$.name") { value("데스크톱") }
        }
    }

    @Test
    fun updateRejectsBlankName() {
        mockMvc.put("/api/items/1") {
            contentType = MediaType.APPLICATION_JSON
            content = """{"name": ""}"""
        }.andExpect {
            status { isBadRequest() }
            jsonPath("$.errors[0].field") { value("name") }
        }
    }

    @Test
    fun updateReturns404WhenMissing() {
        every { updateItemUseCase.update(any()) } throws ItemNotFoundException(99L)

        mockMvc.put("/api/items/99") {
            contentType = MediaType.APPLICATION_JSON
            content = """{"name": "a"}"""
        }.andExpect { status { isNotFound() } }
    }

    @Test
    fun deleteReturns204() {
        // MockK 목은 기본으로 엄격하다. 반환값이 없는(Unit) 메서드도 justRun 으로 허용해 둬야 호출할 수 있다
        justRun { deleteItemUseCase.delete(1L) }

        mockMvc.delete("/api/items/1").andExpect { status { isNoContent() } }

        verify { deleteItemUseCase.delete(1L) }
    }

    @Test
    fun deleteReturns404WhenMissing() {
        every { deleteItemUseCase.delete(99L) } throws ItemNotFoundException(99L)

        mockMvc.delete("/api/items/99").andExpect { status { isNotFound() } }
    }

    private companion object {
        private val NOW: Instant = Instant.parse("2026-01-01T00:00:00Z")
    }
}
