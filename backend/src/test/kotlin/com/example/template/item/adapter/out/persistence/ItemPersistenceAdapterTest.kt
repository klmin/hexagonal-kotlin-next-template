package com.example.template.item.adapter.out.persistence

import com.example.template.TestcontainersConfiguration
import com.example.template.item.domain.Item
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager
import org.springframework.context.annotation.Import
import org.testcontainers.junit.jupiter.Testcontainers
import java.time.Instant
import java.time.temporal.ChronoUnit

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(TestcontainersConfiguration::class, ItemPersistenceAdapter::class)
@Testcontainers(disabledWithoutDocker = true)
class ItemPersistenceAdapterTest {

    // ItemPersistenceAdapter 가 internal 이라 public 프로퍼티로 두면 컴파일 오류(internal 타입 노출). private 으로
    @Autowired
    private lateinit var adapter: ItemPersistenceAdapter

    @Autowired
    private lateinit var em: TestEntityManager

    @Test
    fun saveAndFindById() {
        val saved = adapter.save(Item.create("노트북", "업무용", NOW))

        assertThat(saved.id).isNotNull()
        val found = adapter.findById(saved.id!!)!!
        assertThat(found.name).isEqualTo("노트북")
        assertThat(found.description).isEqualTo("업무용")
        assertThat(found.createdAt).isEqualTo(NOW)
    }

    @Test
    fun updateIsFlushedByDirtyCheckingWithoutSave() {
        val id = adapter.save(Item.create("노트북", "업무용", NOW)).id!!
        em.flush()
        em.clear()

        adapter.update(adapter.findById(id)!!.update("데스크톱", null))
        em.flush()
        em.clear()

        val found = adapter.findById(id)!!
        assertThat(found.name).isEqualTo("데스크톱")
        assertThat(found.description).isNull()
        assertThat(found.createdAt).isEqualTo(NOW)
    }

    @Test
    fun deleteByIdReportsWhetherItExisted() {
        val id = adapter.save(Item.create("노트북", null, NOW)).id!!

        assertThat(adapter.deleteById(id)).isTrue()
        assertThat(adapter.deleteById(id)).isFalse()
        assertThat(adapter.findById(id)).isNull()
    }

    private companion object {
        private val NOW: Instant = Instant.parse("2026-01-01T00:00:00Z").truncatedTo(ChronoUnit.MICROS)
    }
}
