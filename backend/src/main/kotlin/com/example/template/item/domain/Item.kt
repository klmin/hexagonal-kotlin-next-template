package com.example.template.item.domain

import com.example.template.common.error.DomainValidationException
import java.time.Instant

/**
 * 불변 도메인 모델. 값을 바꾸면 새 Item 을 돌려준다.
 *
 * data class 로 만들지 않는 이유: data class 는 copy() 가 생겨서 생성자의 검증을 거치지 않은 Item 을 만들 수 있다.
 * 생성은 create(새 항목) / restore(저장된 항목) 로만 한다.
 */
class Item private constructor(
    val id: Long?,
    name: String,
    description: String?,
    val createdAt: Instant,
) {
    // 생성자 인자를 검증/정리한 값으로 프로퍼티를 만든다
    val name: String = validateName(name)
    val description: String? = validateDescription(description)

    /** 이름, 설명을 바꾼 새 Item 을 돌려준다. 저장된(id 가 있는) Item 만 바꿀 수 있다. */
    fun update(name: String, description: String?): Item {
        // check: 조건이 거짓이면 IllegalStateException
        check(id != null) { "저장되지 않은 Item은 수정할 수 없다" }
        return Item(id, name, description, createdAt)
    }

    companion object {
        // const val: 컴파일 시점 상수. 어노테이션 인자(@Size(max = Item.NAME_MAX_LENGTH))에 쓸 수 있다
        const val NAME_MAX_LENGTH = 100
        const val DESCRIPTION_MAX_LENGTH = 500

        fun create(name: String, description: String?, createdAt: Instant) =
            Item(null, name, description, createdAt)

        // id 가 Long(non-null) 타입이라 null 을 넘기는 코드는 컴파일되지 않는다 (자바는 런타임에 requireNonNull)
        fun restore(id: Long, name: String, description: String?, createdAt: Instant) =
            Item(id, name, description, createdAt)

        private fun validateName(name: String): String {
            if (name.isBlank()) {
                throw DomainValidationException("name must not be blank")
            }
            val trimmed = name.trim()
            if (trimmed.length > NAME_MAX_LENGTH) {
                throw DomainValidationException("name must be at most $NAME_MAX_LENGTH characters")
            }
            return trimmed
        }

        private fun validateDescription(description: String?): String? {
            if (description.isNullOrBlank()) {
                return null
            }
            val trimmed = description.trim()
            if (trimmed.length > DESCRIPTION_MAX_LENGTH) {
                throw DomainValidationException("description must be at most $DESCRIPTION_MAX_LENGTH characters")
            }
            return trimmed
        }
    }
}
