package com.apimetric.core.enums

/**
 * Whether the latest application startup still exposed this route.
 *
 * This is independent of [ApiUsageStatus]. A route can be absent from the current application and
 * still have been called recently.
 */
enum class DiscoveryStatus {
    CURRENTLY_DISCOVERED,
    NO_LONGER_DISCOVERED,
}
