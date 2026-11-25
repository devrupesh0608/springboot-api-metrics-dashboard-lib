package com.apimetric.core.model

import com.apimetric.core.internal.Checks.immutableList
import com.apimetric.core.internal.Checks.nonNegative
import com.apimetric.core.internal.recordEquals
import com.apimetric.core.internal.recordHash
import com.apimetric.core.internal.recordString

/**
 * Latency summary for one query. Percentile fields are estimates from [buckets] and are accepted
 * only when [percentilesApproximate] is true.
 */
class ApiLatencyStatistics(
    sampleCount: Long,
    minDurationMillis: Long?,
    maxDurationMillis: Long?,
    averageDurationMillis: Double?,
    approximateP95DurationMillis: Long?,
    approximateP99DurationMillis: Long?,
    percentilesApproximate: Boolean,
    buckets: List<LatencyBucket>?,
) {
    @get:JvmName("sampleCount")
    val sampleCount: Long

    @get:JvmName("minDurationMillis")
    val minDurationMillis: Long?

    @get:JvmName("maxDurationMillis")
    val maxDurationMillis: Long?

    @get:JvmName("averageDurationMillis")
    val averageDurationMillis: Double?

    @get:JvmName("approximateP95DurationMillis")
    val approximateP95DurationMillis: Long?

    @get:JvmName("approximateP99DurationMillis")
    val approximateP99DurationMillis: Long?

    @get:JvmName("percentilesApproximate")
    val percentilesApproximate: Boolean

    @get:JvmName("buckets")
    val buckets: List<LatencyBucket>

    init {
        val normalizedSampleCount = nonNegative(sampleCount, "sampleCount")
        val normalizedBuckets = immutableList(buckets, "buckets")
        var previousBound = Long.MIN_VALUE
        for (bucket in normalizedBuckets) {
            if (bucket.inclusiveUpperBoundMillis <= previousBound) {
                throw IllegalArgumentException("latency buckets must be in increasing order")
            }
            previousBound = bucket.inclusiveUpperBoundMillis
        }
        val hasPercentile = approximateP95DurationMillis != null || approximateP99DurationMillis != null
        if (hasPercentile && !percentilesApproximate) {
            throw IllegalArgumentException(
                "percentile values are approximate bucket estimates and cannot be marked exact",
            )
        }
        if (hasPercentile && normalizedBuckets.isEmpty()) {
            throw IllegalArgumentException("percentile values require histogram buckets")
        }
        requireNonNegative(approximateP95DurationMillis, "approximateP95DurationMillis")
        requireNonNegative(approximateP99DurationMillis, "approximateP99DurationMillis")
        if (normalizedSampleCount == 0L) {
            if (minDurationMillis != null ||
                maxDurationMillis != null ||
                averageDurationMillis != null ||
                hasPercentile ||
                normalizedBuckets.isNotEmpty()
            ) {
                throw IllegalArgumentException("empty latency statistics cannot include measurements")
            }
        } else {
            if (minDurationMillis == null || maxDurationMillis == null || averageDurationMillis == null) {
                throw IllegalArgumentException("latency statistics require min, max, and average")
            }
            if (minDurationMillis < 0 || maxDurationMillis < 0 || averageDurationMillis < 0) {
                throw IllegalArgumentException("latency measurements must be >= 0")
            }
            if (minDurationMillis > maxDurationMillis) {
                throw IllegalArgumentException("minDurationMillis must be <= maxDurationMillis")
            }
        }
        this.sampleCount = normalizedSampleCount
        this.minDurationMillis = minDurationMillis
        this.maxDurationMillis = maxDurationMillis
        this.averageDurationMillis = averageDurationMillis
        this.approximateP95DurationMillis = approximateP95DurationMillis
        this.approximateP99DurationMillis = approximateP99DurationMillis
        this.percentilesApproximate = percentilesApproximate
        this.buckets = normalizedBuckets
    }

    fun getSampleCount(): Long = sampleCount

    fun getMinDurationMillis(): Long? = minDurationMillis

    fun getMaxDurationMillis(): Long? = maxDurationMillis

    fun getAverageDurationMillis(): Double? = averageDurationMillis

    fun getApproximateP95DurationMillis(): Long? = approximateP95DurationMillis

    fun getApproximateP99DurationMillis(): Long? = approximateP99DurationMillis

    fun isPercentilesApproximate(): Boolean = percentilesApproximate

    fun getBuckets(): List<LatencyBucket> = buckets

    override fun equals(other: Any?): Boolean =
        recordEquals(other) {
            sampleCount == it.sampleCount &&
                minDurationMillis == it.minDurationMillis &&
                maxDurationMillis == it.maxDurationMillis &&
                averageDurationMillis == it.averageDurationMillis &&
                approximateP95DurationMillis == it.approximateP95DurationMillis &&
                approximateP99DurationMillis == it.approximateP99DurationMillis &&
                percentilesApproximate == it.percentilesApproximate &&
                buckets == it.buckets
        }

    override fun hashCode(): Int =
        recordHash(
            sampleCount,
            minDurationMillis,
            maxDurationMillis,
            averageDurationMillis,
            approximateP95DurationMillis,
            approximateP99DurationMillis,
            percentilesApproximate,
            buckets,
        )

    override fun toString(): String =
        recordString(
            "ApiLatencyStatistics",
            "sampleCount" to sampleCount,
            "minDurationMillis" to minDurationMillis,
            "maxDurationMillis" to maxDurationMillis,
            "averageDurationMillis" to averageDurationMillis,
            "approximateP95DurationMillis" to approximateP95DurationMillis,
            "approximateP99DurationMillis" to approximateP99DurationMillis,
            "percentilesApproximate" to percentilesApproximate,
            "buckets" to buckets,
        )

    private fun requireNonNegative(
        value: Long?,
        name: String,
    ) {
        if (value != null && value < 0) {
            throw IllegalArgumentException("$name must be >= 0")
        }
    }
}
