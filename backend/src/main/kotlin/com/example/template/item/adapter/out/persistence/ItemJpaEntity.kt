package com.example.template.item.adapter.out.persistence

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.Instant

/**
 * JPA 엔티티. 어댑터 밖으로 내보내지 않고 도메인 모델(Item)로 바꿔서 돌려준다.
 *
 * - data class 로 만들지 않는다: equals/hashCode 가 모든 필드를 비교해서 지연 로딩, 영속성 컨텍스트와 잘 맞지 않는다
 * - 기본 생성자는 plugin.jpa 가, open 은 allOpen(jakarta.persistence.Entity) 설정이 만들어 준다
 * - protected set: 밖에서는 읽기만 하고 값은 update() 로만 바꾼다 (open 클래스라 private set 은 쓸 수 없다)
 * - internal: 모듈 밖에서 안 보인다. 자바 버전의 package-private 자리지만 범위는 모듈 전체라 더 넓다
 */
@Entity
@Table(name = "items")
internal class ItemJpaEntity(name: String, description: String?, createdAt: Instant) {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null
        protected set

    @Column(nullable = false, length = 100)
    var name: String = name
        protected set

    @Column(length = 500)
    var description: String? = description
        protected set

    @Column(name = "created_at", nullable = false)
    var createdAt: Instant = createdAt
        protected set

    fun update(name: String, description: String?) {
        this.name = name
        this.description = description
    }
}
