package com.apimetric.core.service

import com.apimetric.core.enums.ApiUsageStatus
import com.apimetric.core.enums.DiscoveryStatus
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import java.time.Duration
import java.time.Instant

class ApiUsageClassifierTest {
    @Test
    fun neverCalledWhenNothingWasRecordedEvenIfDiscoveryIsOld() {
        val classification = classify(DISCOVERED_LONG_AGO, null, 0, true)

        assertThat(classification.usageStatus).isEqualTo(ApiUsageStatus.NEVER_CALLED)
        assertThat(classification.discoveryStatus).isEqualTo(DiscoveryStatus.CURRENTLY_DISCOVERED)
    }

    @Test
    fun recentPersistedAccessIsActive() {
        val classification = classify(DISCOVERED_LONG_AGO, EVALUATED_AT.minus(Duration.ofDays(1)), 4, true)

        assertThat(classification.usageStatus).isEqualTo(ApiUsageStatus.ACTIVE)
    }

    @Test
    fun accessOnTheThresholdBoundaryIsActive() {
        assertThat(classify(DISCOVERED_LONG_AGO, ACTIVE_SINCE, 1, true).usageStatus)
            .isEqualTo(ApiUsageStatus.ACTIVE)
    }

    @Test
    fun accessJustOutsideTheThresholdIsPotentiallyUnused() {
        val justOutside = ACTIVE_SINCE.minusNanos(1)

        assertThat(classify(DISCOVERED_LONG_AGO, justOutside, 12, true).usageStatus)
            .isEqualTo(ApiUsageStatus.POTENTIALLY_UNUSED)
    }

    @Test
    fun recordedHitsWithoutLastAccessArePotentiallyUnused() {
        assertThat(classify(DISCOVERED_LONG_AGO, null, 3, true).usageStatus)
            .isEqualTo(ApiUsageStatus.POTENTIALLY_UNUSED)
    }

    @Test
    fun timestampDecidesWhenHitsAndLastAccessDisagree() {
        assertThat(classify(DISCOVERED_LONG_AGO, EVALUATED_AT, 0, true).usageStatus)
            .isEqualTo(ApiUsageStatus.ACTIVE)
        assertThat(classify(DISCOVERED_LONG_AGO, ACTIVE_SINCE.minusNanos(1), 0, true).usageStatus)
            .isEqualTo(ApiUsageStatus.POTENTIALLY_UNUSED)
    }

    @Test
    fun futureLastAccessIsActive() {
        assertThat(classify(DISCOVERED_LONG_AGO, EVALUATED_AT.plus(Duration.ofHours(1)), 1, true).usageStatus)
            .isEqualTo(ApiUsageStatus.ACTIVE)
    }

    @Test
    fun discoveryStateCombinesWithEveryUsageState() {
        assertThat(classify(DISCOVERED_LONG_AGO, null, 0, false))
            .isEqualTo(
                ApiUsageClassification(DiscoveryStatus.NO_LONGER_DISCOVERED, ApiUsageStatus.NEVER_CALLED),
            )
        assertThat(classify(DISCOVERED_LONG_AGO, EVALUATED_AT, 2, false))
            .isEqualTo(
                ApiUsageClassification(DiscoveryStatus.NO_LONGER_DISCOVERED, ApiUsageStatus.ACTIVE),
            )
        assertThat(classify(DISCOVERED_LONG_AGO, ACTIVE_SINCE.minus(Duration.ofDays(1)), 2, false))
            .isEqualTo(
                ApiUsageClassification(DiscoveryStatus.NO_LONGER_DISCOVERED, ApiUsageStatus.POTENTIALLY_UNUSED),
            )
    }

    @Test
    fun rejectsMissingOrInvalidFacts() {
        assertThatThrownBy { classify(null, null, 0, true) }
            .isInstanceOf(NullPointerException::class.java)
            .hasMessage("firstDiscoveredAt")
        assertThatThrownBy {
            ApiUsageClassifier.classify(DISCOVERED_LONG_AGO, null, 0L, true, null, EVALUATED_AT)
        }.isInstanceOf(NullPointerException::class.java)
            .hasMessage("inactivityThreshold")
        assertThatThrownBy {
            ApiUsageClassifier.classify(DISCOVERED_LONG_AGO, null, 0L, true, THRESHOLD, null)
        }.isInstanceOf(NullPointerException::class.java)
            .hasMessage("evaluatedAt")
        assertThatThrownBy {
            ApiUsageClassifier.classify(DISCOVERED_LONG_AGO, null, 0L, true, Duration.ZERO, EVALUATED_AT)
        }.isInstanceOf(IllegalArgumentException::class.java)
            .hasMessageContaining("positive")
        assertThatThrownBy { classify(DISCOVERED_LONG_AGO, null, -1, true) }
            .isInstanceOf(IllegalArgumentException::class.java)
            .hasMessageContaining("totalHits")
    }

    private fun classify(
        firstDiscoveredAt: Instant?,
        lastAccessedAt: Instant?,
        totalHits: Int,
        discovered: Boolean,
    ): ApiUsageClassification =
        ApiUsageClassifier.classify(
            firstDiscoveredAt,
            lastAccessedAt,
            totalHits.toLong(),
            discovered,
            THRESHOLD,
            EVALUATED_AT,
        )

    companion object {
        private val EVALUATED_AT: Instant = Instant.parse("2026-06-15T00:00:00Z")
        private val THRESHOLD: Duration = Duration.ofDays(30)
        private val DISCOVERED_LONG_AGO: Instant = Instant.parse("2025-01-01T00:00:00Z")
        private val ACTIVE_SINCE: Instant = EVALUATED_AT.minus(THRESHOLD)
    }
}
