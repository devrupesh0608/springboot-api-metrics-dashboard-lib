package com.apimetric.core.storage

import com.apimetric.core.enums.TrafficBucketSize
import com.apimetric.core.internal.Checks.required
import com.apimetric.core.internal.recordString
import kotlin.jvm.JvmRecord

/** Traffic series for one API, or for every API when `apiId` is null. */
@JvmRecord
data class TrafficQuery(
    val apiId: Long?,
    val range: TimeRange,
    val bucketSize: TrafficBucketSize,
) {
    init {
        if (apiId != null && apiId <= 0) {
            throw IllegalArgumentException("apiId must be positive")
        }
        required(range, "range")
        required(bucketSize, "bucketSize")
    }

    override fun toString(): String =
        recordString(
            "TrafficQuery",
            "apiId" to apiId,
            "range" to range,
            "bucketSize" to bucketSize,
        )
}
