package com.apimetric.core.model

import com.apimetric.core.enums.ApiUsageStatus
import com.apimetric.core.internal.Checks.required
import com.apimetric.core.internal.recordString
import kotlin.jvm.JvmRecord

/**
 * An API that has never been called, or whose persisted last access is older than the threshold.
 *
 * This is a heuristic. It is not a statement that the API is safe to delete.
 */
@JvmRecord
data class UnusedApiCandidate(
    val api: ApiInventoryEntry,
    val usageStatus: ApiUsageStatus,
) {
    init {
        required(api, "api")
        required(usageStatus, "usageStatus")
        if (usageStatus == ApiUsageStatus.ACTIVE) {
            throw IllegalArgumentException("an active API is not an unused candidate")
        }
    }

    override fun toString(): String = recordString("UnusedApiCandidate", "api" to api, "usageStatus" to usageStatus)
}
