package com.apimetric.core.enums

/**
 * Call history relative to the configured inactivity threshold.
 *
 * The decision uses persisted hit counts and `last_accessed_at`. Hits observed only in the
 * current process are not an input.
 */
enum class ApiUsageStatus {
    /** No invocation has been recorded since the API was discovered. */
    NEVER_CALLED,

    /** The persisted last access falls inside the inactivity threshold. */
    ACTIVE,

    /**
     * An invocation was recorded, and the persisted last access is older than the threshold or
     * missing.
     */
    POTENTIALLY_UNUSED,
}
