package com.apimetric.core.storage

import com.apimetric.core.internal.Checks.positiveDuration
import com.apimetric.core.internal.Checks.required
import com.apimetric.core.internal.recordEquals
import com.apimetric.core.internal.recordHash
import com.apimetric.core.internal.recordString
import java.time.Duration
import java.time.Instant

/**
 * Request totals over `requestRange`, plus inventory counts evaluated at `evaluatedAt`.
 */
class UsageSnapshotQuery(
    requestRange: TimeRange?,
    inactivityThreshold: Duration?,
    evaluatedAt: Instant?,
) {
    @get:JvmName("requestRange")
    val requestRange: TimeRange

    @get:JvmName("inactivityThreshold")
    val inactivityThreshold: Duration

    @get:JvmName("evaluatedAt")
    val evaluatedAt: Instant

    init {
        this.requestRange = required(requestRange, "requestRange")
        this.inactivityThreshold = positiveDuration(inactivityThreshold, "inactivityThreshold")
        this.evaluatedAt = required(evaluatedAt, "evaluatedAt")
    }

    fun getRequestRange(): TimeRange = requestRange

    fun getInactivityThreshold(): Duration = inactivityThreshold

    fun getEvaluatedAt(): Instant = evaluatedAt

    override fun equals(other: Any?): Boolean =
        recordEquals(other) {
            requestRange == it.requestRange &&
                inactivityThreshold == it.inactivityThreshold &&
                evaluatedAt == it.evaluatedAt
        }

    override fun hashCode(): Int = recordHash(requestRange, inactivityThreshold, evaluatedAt)

    override fun toString(): String =
        recordString(
            "UsageSnapshotQuery",
            "requestRange" to requestRange,
            "inactivityThreshold" to inactivityThreshold,
            "evaluatedAt" to evaluatedAt,
        )
}
