package com.apimetric.core.storage

import com.apimetric.core.internal.Checks.required
import com.apimetric.core.internal.recordString
import java.time.Instant
import kotlin.jvm.JvmRecord

/** Half-open instant range: start inclusive, end exclusive. */
@JvmRecord
data class TimeRange(
    val startInclusive: Instant,
    val endExclusive: Instant,
) {
    init {
        required(startInclusive, "startInclusive")
        required(endExclusive, "endExclusive")
        if (!startInclusive.isBefore(endExclusive)) {
            throw IllegalArgumentException("startInclusive must be before endExclusive")
        }
    }

    fun contains(instant: Instant?): Boolean {
        val checked = required(instant, "instant")
        return !checked.isBefore(startInclusive) && checked.isBefore(endExclusive)
    }

    override fun toString(): String =
        recordString(
            "TimeRange",
            "startInclusive" to startInclusive,
            "endExclusive" to endExclusive,
        )
}
