package com.example.template.common.config

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class ClockConfigTest {

    @Test
    fun clockHasMicrosecondPrecisionLikePostgres() {
        val clock = ClockConfig().clock()
        repeat(100) {
            assertThat(clock.instant().nano % 1_000).isZero()
        }
    }
}
