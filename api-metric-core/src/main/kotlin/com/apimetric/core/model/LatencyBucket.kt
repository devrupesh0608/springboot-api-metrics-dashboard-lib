package com.apimetric.core.model

import com.apimetric.core.internal.Checks.nonNegative
import com.apimetric.core.internal.recordEquals
import com.apimetric.core.internal.recordHash
import com.apimetric.core.internal.recordString

/** One histogram bucket. `inclusiveUpperBoundMillis` is the bucket's upper edge. */
class LatencyBucket(
    inclusiveUpperBoundMillis: Long,
    count: Long,
) {
    @get:JvmName("inclusiveUpperBoundMillis")
    val inclusiveUpperBoundMillis: Long

    @get:JvmName("count")
    val count: Long

    init {
        this.inclusiveUpperBoundMillis = nonNegative(inclusiveUpperBoundMillis, "inclusiveUpperBoundMillis")
        this.count = nonNegative(count, "count")
    }

    fun getInclusiveUpperBoundMillis(): Long = inclusiveUpperBoundMillis

    fun getCount(): Long = count

    override fun equals(other: Any?): Boolean =
        recordEquals(other) {
            inclusiveUpperBoundMillis == it.inclusiveUpperBoundMillis && count == it.count
        }

    override fun hashCode(): Int = recordHash(inclusiveUpperBoundMillis, count)

    override fun toString(): String =
        recordString(
            "LatencyBucket",
            "inclusiveUpperBoundMillis" to inclusiveUpperBoundMillis,
            "count" to count,
        )
}
