package com.apimetric.core.model

import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test

class ApiLatencyStatisticsTest {
    @Test
    fun percentileValuesMustBeLabeledApproximate() {
        val buckets = listOf(LatencyBucket(100L, 95L), LatencyBucket(500L, 5L))

        assertThatThrownBy {
            ApiLatencyStatistics(100L, 1L, 500L, 20.0, 100L, 500L, false, buckets)
        }.isInstanceOf(IllegalArgumentException::class.java)
            .hasMessageContaining("approximate")
    }

    @Test
    fun emptySampleRejectsMeasurements() {
        assertThatThrownBy {
            ApiLatencyStatistics(0L, 0L, 0L, 0.0, null, null, false, listOf())
        }.isInstanceOf(IllegalArgumentException::class.java)
            .hasMessageContaining("empty latency statistics")
    }
}
