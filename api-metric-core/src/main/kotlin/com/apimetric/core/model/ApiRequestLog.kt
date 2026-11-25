package com.apimetric.core.model

import com.apimetric.core.internal.Checks.blankToNull
import com.apimetric.core.internal.Checks.nonNegative
import com.apimetric.core.internal.Checks.positive
import com.apimetric.core.internal.Checks.requireHttpStatus
import com.apimetric.core.internal.Checks.required
import com.apimetric.core.internal.recordEquals
import com.apimetric.core.internal.recordHash
import com.apimetric.core.internal.recordString
import java.time.Instant

/** One retained invocation loaded for the request inspector. */
class ApiRequestLog(
    id: Long,
    apiId: Long,
    identity: ApiIdentity?,
    httpStatus: Int,
    durationMillis: Long,
    correlationId: String?,
    requestPayload: CapturedPayload?,
    responsePayload: CapturedPayload?,
    requestHeaders: String?,
    responseHeaders: String?,
    createdAt: Instant?,
) {
    @get:JvmName("id")
    val id: Long

    @get:JvmName("apiId")
    val apiId: Long

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

    @get:JvmName("createdAt")
    val createdAt: Instant

    init {
        val normalizedId = positive(id, "id")
        val normalizedApiId = positive(apiId, "apiId")
        val checkedIdentity = required(identity, "identity")
        val normalizedStatus = requireHttpStatus(httpStatus)
        val normalizedDuration = nonNegative(durationMillis, "durationMillis")
        val normalizedCorrelation = blankToNull(correlationId)
        val checkedRequest = required(requestPayload, "requestPayload")
        val checkedResponse = required(responsePayload, "responsePayload")
        val normalizedRequestHeaders = blankToNull(requestHeaders)
        val normalizedResponseHeaders = blankToNull(responseHeaders)
        val checkedCreatedAt = required(createdAt, "createdAt")
        this.id = normalizedId
        this.apiId = normalizedApiId
        this.identity = checkedIdentity
        this.httpStatus = normalizedStatus
        this.durationMillis = normalizedDuration
        this.correlationId = normalizedCorrelation
        this.requestPayload = checkedRequest
        this.responsePayload = checkedResponse
        this.requestHeaders = normalizedRequestHeaders
        this.responseHeaders = normalizedResponseHeaders
        this.createdAt = checkedCreatedAt
    }

    fun getId(): Long = id

    fun getApiId(): Long = apiId

    fun getIdentity(): ApiIdentity = identity

    fun getHttpStatus(): Int = httpStatus

    fun getDurationMillis(): Long = durationMillis

    fun getCorrelationId(): String? = correlationId

    fun getRequestPayload(): CapturedPayload = requestPayload

    fun getResponsePayload(): CapturedPayload = responsePayload

    fun getRequestHeaders(): String? = requestHeaders

    fun getResponseHeaders(): String? = responseHeaders

    fun getCreatedAt(): Instant = createdAt

    override fun equals(other: Any?): Boolean =
        recordEquals(other) {
            id == it.id &&
                apiId == it.apiId &&
                identity == it.identity &&
                httpStatus == it.httpStatus &&
                durationMillis == it.durationMillis &&
                correlationId == it.correlationId &&
                requestPayload == it.requestPayload &&
                responsePayload == it.responsePayload &&
                requestHeaders == it.requestHeaders &&
                responseHeaders == it.responseHeaders &&
                createdAt == it.createdAt
        }

    override fun hashCode(): Int =
        recordHash(
            id,
            apiId,
            identity,
            httpStatus,
            durationMillis,
            correlationId,
            requestPayload,
            responsePayload,
            requestHeaders,
            responseHeaders,
            createdAt,
        )

    override fun toString(): String =
        recordString(
            "ApiRequestLog",
            "id" to id,
            "apiId" to apiId,
            "identity" to identity,
            "httpStatus" to httpStatus,
            "durationMillis" to durationMillis,
            "correlationId" to correlationId,
            "requestPayload" to requestPayload,
            "responsePayload" to responsePayload,
            "requestHeaders" to requestHeaders,
            "responseHeaders" to responseHeaders,
            "createdAt" to createdAt,
        )
}
