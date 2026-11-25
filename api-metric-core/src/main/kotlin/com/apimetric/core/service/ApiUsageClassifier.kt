package com.apimetric.core.service

import com.apimetric.core.enums.ApiUsageStatus
import com.apimetric.core.enums.DiscoveryStatus
import com.apimetric.core.internal.Checks.required
import java.time.Duration
import java.time.Instant

/**
 * Classifies an API from persisted discovery and access facts.
 *
 * `firstDiscoveredAt` is required so callers pass the stored discovery time. Age since discovery
 * does not by itself make an API unused, and hits that exist only in the current process are not
 * an input.
 */
class ApiUsageClassifier private constructor() {
    companion object {
        @JvmStatic
        fun classify(
            firstDiscoveredAt: Instant?,
            lastAccessedAt: Instant?,
            totalHits: Long,
            currentlyDiscovered: Boolean,
            inactivityThreshold: Duration?,
            evaluatedAt: Instant?,
        ): ApiUsageClassification {
            required(firstDiscoveredAt, "firstDiscoveredAt")
            val threshold = required(inactivityThreshold, "inactivityThreshold")
            val evaluated = required(evaluatedAt, "evaluatedAt")
            if (threshold.isZero || threshold.isNegative) {
                throw IllegalArgumentException("inactivityThreshold must be positive")
            }
            if (totalHits < 0) {
                throw IllegalArgumentException("totalHits must be >= 0")
            }
            val discoveryStatus =
                if (currentlyDiscovered) {
                    DiscoveryStatus.CURRENTLY_DISCOVERED
                } else {
                    DiscoveryStatus.NO_LONGER_DISCOVERED
                }
            return ApiUsageClassification(
                discoveryStatus,
                usageStatus(lastAccessedAt, totalHits, threshold, evaluated),
            )
        }

        private fun usageStatus(
            lastAccessedAt: Instant?,
            totalHits: Long,
            inactivityThreshold: Duration,
            evaluatedAt: Instant,
        ): ApiUsageStatus {
            if (totalHits == 0L && lastAccessedAt == null) {
                return ApiUsageStatus.NEVER_CALLED
            }
            if (lastAccessedAt == null) {
                return ApiUsageStatus.POTENTIALLY_UNUSED
            }
            val activeSince = evaluatedAt.minus(inactivityThreshold)
            if (!lastAccessedAt.isBefore(activeSince)) {
                return ApiUsageStatus.ACTIVE
            }
            return ApiUsageStatus.POTENTIALLY_UNUSED
        }
    }
}
