package com.apimetric.core.model

import com.apimetric.core.enums.CaptureStatus
import com.apimetric.core.internal.Checks.blankToNull
import com.apimetric.core.internal.Checks.required
import com.apimetric.core.internal.recordEquals
import com.apimetric.core.internal.recordHash
import com.apimetric.core.internal.recordString

/**
 * Sanitized request or response text that is eligible to be stored.
 *
 * [CaptureStatus.CAPTURED] and [CaptureStatus.TRUNCATED] carry the retained text. Every other
 * status carries no body.
 */
class CapturedPayload(
    status: CaptureStatus?,
    contentType: String?,
    body: String?,
) {
    @get:JvmName("status")
    val status: CaptureStatus

    @get:JvmName("contentType")
    val contentType: String?

    @get:JvmName("body")
    val body: String?

    init {
        val checkedStatus = required(status, "status")
        val normalizedContentType = blankToNull(contentType)
        when (checkedStatus) {
            CaptureStatus.CAPTURED, CaptureStatus.TRUNCATED -> {
                if (body == null) {
                    throw IllegalArgumentException("body is required when status is $checkedStatus")
                }
            }
            CaptureStatus.NOT_CAPTURED, CaptureStatus.EXCLUDED, CaptureStatus.UNSUPPORTED -> {
                if (body != null) {
                    throw IllegalArgumentException("body must be absent when status is $checkedStatus")
                }
            }
        }
        this.status = checkedStatus
        this.contentType = normalizedContentType
        this.body = body
    }

    fun getStatus(): CaptureStatus = status

    fun getContentType(): String? = contentType

    fun getBody(): String? = body

    override fun equals(other: Any?): Boolean =
        recordEquals(other) {
            status == it.status && contentType == it.contentType && body == it.body
        }

    override fun hashCode(): Int = recordHash(status, contentType, body)

    override fun toString(): String =
        recordString(
            "CapturedPayload",
            "status" to status,
            "contentType" to contentType,
            "body" to body,
        )

    companion object {
        @JvmStatic
        fun notCaptured(): CapturedPayload = CapturedPayload(CaptureStatus.NOT_CAPTURED, null, null)

        @JvmStatic
        fun excluded(contentType: String?): CapturedPayload = CapturedPayload(CaptureStatus.EXCLUDED, contentType, null)

        @JvmStatic
        fun unsupported(contentType: String?): CapturedPayload = CapturedPayload(CaptureStatus.UNSUPPORTED, contentType, null)

        @JvmStatic
        fun captured(
            contentType: String?,
            body: String?,
        ): CapturedPayload = CapturedPayload(CaptureStatus.CAPTURED, contentType, body)

        @JvmStatic
        fun truncated(
            contentType: String?,
            body: String?,
        ): CapturedPayload = CapturedPayload(CaptureStatus.TRUNCATED, contentType, body)
    }
}
