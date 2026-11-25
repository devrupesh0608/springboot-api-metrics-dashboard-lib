package com.apimetric.core.model

import com.apimetric.core.enums.TrafficBucketSize
import com.apimetric.core.internal.Checks.immutableList
import com.apimetric.core.internal.Checks.required
import com.apimetric.core.internal.recordEquals
import com.apimetric.core.internal.recordHash
import com.apimetric.core.internal.recordString

/** Ordered traffic buckets for a dashboard chart. */
class TrafficSeries(
    bucketSize: TrafficBucketSize?,
    points: List<TrafficPoint>?,
) {
    @get:JvmName("bucketSize")
    val bucketSize: TrafficBucketSize

    @get:JvmName("points")
    val points: List<TrafficPoint>

    init {
        this.bucketSize = required(bucketSize, "bucketSize")
        this.points = immutableList(points, "points")
    }

    fun getBucketSize(): TrafficBucketSize = bucketSize

    fun getPoints(): List<TrafficPoint> = points

    override fun equals(other: Any?): Boolean =
        recordEquals(other) {
            bucketSize == it.bucketSize && points == it.points
        }

    override fun hashCode(): Int = recordHash(bucketSize, points)

    override fun toString(): String = recordString("TrafficSeries", "bucketSize" to bucketSize, "points" to points)
}
