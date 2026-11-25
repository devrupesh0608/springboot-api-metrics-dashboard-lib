package com.apimetric.core.model

import com.apimetric.core.internal.Checks.nonNegative
import com.apimetric.core.internal.Checks.required
import com.apimetric.core.internal.recordEquals
import com.apimetric.core.internal.recordHash
import com.apimetric.core.internal.recordString
import com.apimetric.core.storage.TimeRange

/** Point-in-time inventory counts plus request totals for a selected range. */
class ApiUsageSnapshot(
    totalRequests: Long,
    failedRequests: Long,
    discoveredApiCount: Long,
    activeApiCount: Long,
    neverCalledApiCount: Long,
    unusedCandidateCount: Long,
    totalHits: Long,
    requestRange: TimeRange?,
) {
    @get:JvmName("totalRequests")
    val totalRequests: Long

    @get:JvmName("failedRequests")
    val failedRequests: Long

    @get:JvmName("discoveredApiCount")
    val discoveredApiCount: Long

    @get:JvmName("activeApiCount")
    val activeApiCount: Long

    @get:JvmName("neverCalledApiCount")
    val neverCalledApiCount: Long

    @get:JvmName("unusedCandidateCount")
    val unusedCandidateCount: Long

    @get:JvmName("totalHits")
    val totalHits: Long

    @get:JvmName("requestRange")
    val requestRange: TimeRange

    init {
        val normalizedTotalRequests = nonNegative(totalRequests, "totalRequests")
        val normalizedFailedRequests = nonNegative(failedRequests, "failedRequests")
        if (normalizedFailedRequests > normalizedTotalRequests) {
            throw IllegalArgumentException("failedRequests cannot exceed totalRequests")
        }
        val normalizedDiscovered = nonNegative(discoveredApiCount, "discoveredApiCount")
        val normalizedActive = nonNegative(activeApiCount, "activeApiCount")
        val normalizedNeverCalled = nonNegative(neverCalledApiCount, "neverCalledApiCount")
        val normalizedUnused = nonNegative(unusedCandidateCount, "unusedCandidateCount")
        val normalizedHits = nonNegative(totalHits, "totalHits")
        val checkedRange = required(requestRange, "requestRange")
        this.totalRequests = normalizedTotalRequests
        this.failedRequests = normalizedFailedRequests
        this.discoveredApiCount = normalizedDiscovered
        this.activeApiCount = normalizedActive
        this.neverCalledApiCount = normalizedNeverCalled
        this.unusedCandidateCount = normalizedUnused
        this.totalHits = normalizedHits
        this.requestRange = checkedRange
    }

    /** Failed requests divided by total requests. Zero when the range has no requests. */
    fun errorRate(): Double {
        if (totalRequests == 0L) {
            return 0.0
        }
        return failedRequests.toDouble() / totalRequests.toDouble()
    }

    fun getTotalRequests(): Long = totalRequests

    fun getFailedRequests(): Long = failedRequests

    fun getDiscoveredApiCount(): Long = discoveredApiCount

    fun getActiveApiCount(): Long = activeApiCount

    fun getNeverCalledApiCount(): Long = neverCalledApiCount

    fun getUnusedCandidateCount(): Long = unusedCandidateCount

    fun getTotalHits(): Long = totalHits

    fun getRequestRange(): TimeRange = requestRange

    override fun equals(other: Any?): Boolean =
        recordEquals(other) {
            totalRequests == it.totalRequests &&
                failedRequests == it.failedRequests &&
                discoveredApiCount == it.discoveredApiCount &&
                activeApiCount == it.activeApiCount &&
                neverCalledApiCount == it.neverCalledApiCount &&
                unusedCandidateCount == it.unusedCandidateCount &&
                totalHits == it.totalHits &&
                requestRange == it.requestRange
        }

    override fun hashCode(): Int =
        recordHash(
            totalRequests,
            failedRequests,
            discoveredApiCount,
            activeApiCount,
            neverCalledApiCount,
            unusedCandidateCount,
            totalHits,
            requestRange,
        )

    override fun toString(): String =
        recordString(
            "ApiUsageSnapshot",
            "totalRequests" to totalRequests,
            "failedRequests" to failedRequests,
            "discoveredApiCount" to discoveredApiCount,
            "activeApiCount" to activeApiCount,
            "neverCalledApiCount" to neverCalledApiCount,
            "unusedCandidateCount" to unusedCandidateCount,
            "totalHits" to totalHits,
            "requestRange" to requestRange,
        )
}
