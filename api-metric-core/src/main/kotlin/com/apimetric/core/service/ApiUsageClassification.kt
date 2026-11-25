package com.apimetric.core.service

import com.apimetric.core.enums.ApiUsageStatus
import com.apimetric.core.enums.DiscoveryStatus
import com.apimetric.core.internal.Checks.required
import com.apimetric.core.internal.recordString
import kotlin.jvm.JvmRecord

/** Discovery presence and call-history classification for one API at one instant. */
@JvmRecord
data class ApiUsageClassification(
    val discoveryStatus: DiscoveryStatus,
    val usageStatus: ApiUsageStatus,
) {
    init {
        required(discoveryStatus, "discoveryStatus")
        required(usageStatus, "usageStatus")
    }

    override fun toString(): String =
        recordString(
            "ApiUsageClassification",
            "discoveryStatus" to discoveryStatus,
            "usageStatus" to usageStatus,
        )
}
