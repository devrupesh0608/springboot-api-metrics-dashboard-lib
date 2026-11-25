package com.apimetric.core.enums

/** How much of a request or response body was kept. */
enum class CaptureStatus {
    /** Capture is disabled, the body was empty of retained text, or no body was available. */
    NOT_CAPTURED,

    /** The retained text is the full captured body within the size limit. */
    CAPTURED,

    /** The retained text was cut at the configured size limit. */
    TRUNCATED,

    /** The body was skipped because the path or content type is excluded. */
    EXCLUDED,

    /** The body was skipped because the content is binary, multipart, or streaming. */
    UNSUPPORTED,
}
