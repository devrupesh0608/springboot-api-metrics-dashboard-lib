package com.apimetric.core.model

import com.apimetric.core.internal.Checks.blankToNull
import com.apimetric.core.internal.Checks.nonNegative
import com.apimetric.core.internal.Checks.requireHttpStatus
import com.apimetric.core.internal.Checks.required
import com.apimetric.core.internal.recordEquals
import com.apimetric.core.internal.recordHash
import com.apimetric.core.internal.recordString
import java.time.Instant

/**
 * One completed API invocation ready for asynchronous persistence.
 *
 * A status from 200 inclusive to 400 exclusive counts as a successful hit. Every other retained
 * status counts as a failed hit.
 */
class ApiRequestEvent(
    identity: ApiIdentity?,
    httpStatus: Int,
    durationMillis: Long,
    correlationId: String?,
    requestPayload: CapturedPayload?,
    responsePayload: CapturedPayload?,
    requestHeaders: String?,
    responseHeaders: String?,
    occurredAt: Instant?,
) {
    @get:JvmName("identity")
    val identity: ApiIdentity

    @get:JvmName("httpStatus")
    val httpStatus: Int

    @get:JvmName("durationMillis")
    val durationMillis: Long

    @get:JvmName("correlationId")
    val correlationId: String?

    @get:JvmName("requestPayload")
    val requestPayload: CapturedPayload

    @get:JvmName("responsePayload")
    val responsePayload: CapturedPayload

    @get:JvmName("requestHeaders")
    val requestHeaders: String?

    @get:JvmName("responseHeaders")
    val responseHeaders: String?

    @get:JvmName("occurredAt")
    val occurredAt: Instant

    init {
        val checkedIdentity = required(identity, "identity")
        val normalizedStatus = requireHttpStatus(httpStatus)
        val normalizedDuration = nonNegative(durationMillis, "durationMillis")
        val normalizedCorrelation = blankToNull(correlationId)
        val checkedRequest = required(requestPayload, "requestPayload")
        val checkedResponse = required(responsePayload, "responsePayload")
        val normalizedRequestHeaders = blankToNull(requestHeaders)
        val normalizedResponseHeaders = blankToNull(responseHeaders)
        val checkedOccurredAt = required(occurredAt, "occurredAt")
        this.identity = checkedIdentity
        this.httpStatus = normalizedStatus
        this.durationMillis = normalizedDuration
        this.correlationId = normalizedCorrelation
        this.requestPayload = checkedRequest
        this.responsePayload = checkedResponse
        this.requestHeaders = normalizedRequestHeaders
        this.responseHeaders = normalizedResponseHeaders
        this.occurredAt = checkedOccurredAt
    }

    fun successful(): Boolean = httpStatus >= 200 && httpStatus < 400

    fun getIdentity(): ApiIdentity = identity

    fun getHttpStatus(): Int = httpStatus

    fun getDurationMillis(): Long = durationMillis

    fun getCorrelationId(): String? = correlationId

    fun getRequestPayload(): CapturedPayload = requestPayload

    fun getResponsePayload(): CapturedPayload = responsePayload

    fun getRequestHeaders(): String? = requestHeaders

    fun getResponseHeaders(): String? = responseHeaders

    fun getOccurredAt(): Instant = occurredAt

    override fun equals(other: Any?): Boolean =
        recordEquals(other) {
            identity == it.identity &&
                httpStatus == it.httpStatus &&
                durationMillis == it.durationMillis &&
                correlationId == it.correlationId &&
                requestPayload == it.requestPayload &&
                responsePayload == it.responsePayload &&
                requestHeaders == it.requestHeaders &&
                responseHeaders == it.responseHeaders &&
                occurredAt == it.occurredAt
        }

    override fun hashCode(): Int =
        recordHash(
            identity,
            httpStatus,
            durationMillis,
            correlationId,
            requestPayload,
            responsePayload,
            requestHeaders,
            responseHeaders,
            occurredAt,
        )

    override fun toString(): String =
        recordString(
            "ApiRequestEvent",
            "identity" to identity,
            "httpStatus" to httpStatus,
            "durationMillis" to durationMillis,
            "correlationId" to correlationId,
            "requestPayload" to requestPayload,
            "responsePayload" to responsePayload,
            "requestHeaders" to requestHeaders,
            "responseHeaders" to responseHeaders,
            "occurredAt" to occurredAt,
        )
}
