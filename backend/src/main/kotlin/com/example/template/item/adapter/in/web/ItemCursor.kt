package com.example.template.item.adapter.`in`.web

import com.example.template.common.error.DomainValidationException
import java.util.Base64

// 클라이언트에는 커서 내용을 숨긴다(불투명 문자열). 나중에 정렬 기준이 바뀌어도 API 형태는 그대로 둘 수 있다
// object: 싱글턴. 자바의 private 생성자 + static 메서드 유틸 클래스 자리
internal object ItemCursor {

    private val encoder = Base64.getUrlEncoder().withoutPadding()
    private val decoder = Base64.getUrlDecoder()

    fun encode(id: Long?): String? = id?.let { encoder.encodeToString(it.toString().toByteArray()) }

    fun decode(cursor: String?): Long? {
        if (cursor.isNullOrBlank()) {
            return null
        }
        val id = try {
            String(decoder.decode(cursor)).toLong()
        } catch (e: IllegalArgumentException) {
            // Base64 오류와 숫자 변환 오류(NumberFormatException)가 모두 IllegalArgumentException
            throw DomainValidationException("cursor is invalid")
        }
        if (id <= 0) {
            throw DomainValidationException("cursor is invalid")
        }
        return id
    }
}
