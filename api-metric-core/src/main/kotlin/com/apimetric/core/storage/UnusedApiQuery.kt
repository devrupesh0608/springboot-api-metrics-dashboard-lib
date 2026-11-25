package com.apimetric.core.storage

import com.apimetric.core.internal.Checks.positiveDuration
import com.apimetric.core.internal.Checks.required
import com.apimetric.core.internal.recordEquals
import com.apimetric.core.internal.recordHash
import com.apimetric.core.internal.recordString
import java.time.Duration
import java.time.Instant

/** Inputs for the unused-candidate query. */
class UnusedApiQuery(
    inactivityThreshold: Duration?,
    evaluatedAt: Instant?,
) {
    @get:JvmName("inactivityThreshold")
    val inactivityThreshold: Duration

    @get:JvmName("evaluatedAt")
    val evaluatedAt: Instant

    init {
        this.inactivityThreshold = positiveDuration(inactivityThreshold, "inactivityThreshold")
        this.evaluatedAt = required(evaluatedAt, "evaluatedAt")
    }

    fun getInactivityThreshold(): Duration = inactivityThreshold

    fun getEvaluatedAt(): Instant = evaluatedAt

    override fun equals(other: Any?): Boolean =
        recordEquals(other) {
            inactivityThreshold == it.inactivityThreshold && evaluatedAt == it.evaluatedAt
        }

    override fun hashCode(): Int = recordHash(inactivityThreshold, evaluatedAt)

    override fun toString(): String =
        recordString(
            "UnusedApiQuery",
            "inactivityThreshold" to inactivityThreshold,
            "evaluatedAt" to evaluatedAt,
        )
}
