package com.example.template.common.config

import com.example.template.TestcontainersConfiguration
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.Import
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.transaction.annotation.Transactional
import org.testcontainers.junit.jupiter.Testcontainers
import java.io.IOException
import java.time.OffsetDateTime

// 코틀린에는 checked 예외가 없어서 IOException 같은 Exception 도 선언 없이 던져진다.
// Spring 기본값은 RuntimeException/Error 에서만 롤백하므로, TransactionConfig 가 없으면 아래 insert 가 커밋된다
@SpringBootTest
@Import(TestcontainersConfiguration::class, TransactionConfigTest.CheckedFailingWriter::class)
@Testcontainers(disabledWithoutDocker = true)
class TransactionConfigTest {

    @Autowired
    private lateinit var writer: CheckedFailingWriter

    @Autowired
    private lateinit var jdbc: JdbcTemplate

    @Test
    fun checkedExceptionRollsBack() {
        assertThatThrownBy { writer.insertThenFail() }.isInstanceOf(IOException::class.java)

        val count = jdbc.queryForObject("select count(*) from items where name = ?", Long::class.java, NAME)
        assertThat(count).isZero()
    }

    // @Transactional 이 붙은 클래스라 plugin.spring 이 open 으로 만들어 프록시가 생긴다
    @Transactional
    class CheckedFailingWriter(private val jdbc: JdbcTemplate) {
        fun insertThenFail() {
            jdbc.update("insert into items (name, created_at) values (?, ?)", NAME, CREATED_AT)
            throw IOException("디스크 쓰기 실패")
        }
    }

    private companion object {
        const val NAME = "rollback-probe"
        val CREATED_AT: OffsetDateTime = OffsetDateTime.parse("2026-01-01T00:00:00Z")
    }
}
