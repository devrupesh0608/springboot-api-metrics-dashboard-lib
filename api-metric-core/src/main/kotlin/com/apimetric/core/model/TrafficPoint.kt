package com.apimetric.core.model

import com.apimetric.core.internal.Checks.nonNegative
import com.apimetric.core.internal.Checks.required
import com.apimetric.core.internal.recordEquals
import com.apimetric.core.internal.recordHash
import com.apimetric.core.internal.recordString
import java.time.Instant

/** Hits and duration accumulated in one time bucket. */
class TrafficPoint(
    bucketStart: Instant?,
    totalHits: Long,
    successfulHits: Long,
    failedHits: Long,
    totalDurationMillis: Long,
) {
    @get:JvmName("bucketStart")
    val bucketStart: Instant

    @get:JvmName("totalHits")
    val totalHits: Long

    @get:JvmName("successfulHits")
    val successfulHits: Long

    @get:JvmName("failedHits")
    val failedHits: Long

    @get:JvmName("totalDurationMillis")
    val totalDurationMillis: Long

    init {
        val checkedStart = required(bucketStart, "bucketStart")
        val normalizedTotal = nonNegative(totalHits, "totalHits")
        val normalizedSuccessful = nonNegative(successfulHits, "successfulHits")
        val normalizedFailed = nonNegative(failedHits, "failedHits")
        val normalizedDuration = nonNegative(totalDurationMillis, "totalDurationMillis")
        if (normalizedSuccessful + normalizedFailed != normalizedTotal) {
            throw IllegalArgumentException("successfulHits and failedHits must add up to totalHits")
        }
        this.bucketStart = checkedStart
        this.totalHits = normalizedTotal
        this.successfulHits = normalizedSuccessful
        this.failedHits = normalizedFailed
        this.totalDurationMillis = normalizedDuration
    }

    fun getBucketStart(): Instant = bucketStart

    fun getTotalHits(): Long = totalHits

    fun getSuccessfulHits(): Long = successfulHits

    fun getFailedHits(): Long = failedHits

    fun getTotalDurationMillis(): Long = totalDurationMillis

    override fun equals(other: Any?): Boolean =
        recordEquals(other) {
            bucketStart == it.bucketStart &&
                totalHits == it.totalHits &&
                successfulHits == it.successfulHits &&
                failedHits == it.failedHits &&
                totalDurationMillis == it.totalDurationMillis
        }

    override fun hashCode(): Int = recordHash(bucketStart, totalHits, successfulHits, failedHits, totalDurationMillis)

    override fun toString(): String =
        recordString(
            "TrafficPoint",
            "bucketStart" to bucketStart,
            "totalHits" to totalHits,
            "successfulHits" to successfulHits,
            "failedHits" to failedHits,
            "totalDurationMillis" to totalDurationMillis,
        )
}
