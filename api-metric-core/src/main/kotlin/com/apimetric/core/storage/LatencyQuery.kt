package com.apimetric.core.storage

import com.apimetric.core.internal.Checks.positive
import com.apimetric.core.internal.Checks.required
import com.apimetric.core.internal.recordEquals
import com.apimetric.core.internal.recordHash
import com.apimetric.core.internal.recordString

/** Latency summary for one API over a half-open time range. */
class LatencyQuery(
    apiId: Long,
    range: TimeRange?,
) {
    @get:JvmName("apiId")
    val apiId: Long

    @get:JvmName("range")
    val range: TimeRange

    init {
        this.apiId = positive(apiId, "apiId")
        this.range = required(range, "range")
    }

    fun getApiId(): Long = apiId

    fun getRange(): TimeRange = range

    override fun equals(other: Any?): Boolean =
        recordEquals(other) {
            apiId == it.apiId && range == it.range
        }

    override fun hashCode(): Int = recordHash(apiId, range)

    override fun toString(): String = recordString("LatencyQuery", "apiId" to apiId, "range" to range)
}
