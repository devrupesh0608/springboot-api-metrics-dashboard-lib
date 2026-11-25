package com.apimetric.core.storage

import com.apimetric.core.internal.Checks.required
import com.apimetric.core.internal.recordEquals
import com.apimetric.core.internal.recordHash
import com.apimetric.core.internal.recordString
import java.util.Locale

/** Filter and page for retained request logs. A null bound leaves that side open. */
class RequestLogQuery(
    apiId: Long?,
    httpMethod: String?,
    minimumStatus: Int?,
    maximumStatus: Int?,
    createdAt: TimeRange?,
    text: String?,
    page: PageRequest?,
) {
    @get:JvmName("apiId")
    val apiId: Long?

    @get:JvmName("httpMethod")
    val httpMethod: String?

    @get:JvmName("minimumStatus")
    val minimumStatus: Int?

    @get:JvmName("maximumStatus")
    val maximumStatus: Int?

    @get:JvmName("createdAt")
    val createdAt: TimeRange?

    @get:JvmName("text")
    val text: String?

    @get:JvmName("page")
    val page: PageRequest

    init {
        if (apiId != null && apiId <= 0) {
            throw IllegalArgumentException("apiId must be positive")
        }
        val normalizedMethod =
            if (httpMethod == null || httpMethod.isBlank()) {
                null
            } else {
                httpMethod.trim().uppercase(Locale.ROOT)
            }
        if (normalizedMethod != null && !normalizedMethod.matches(Regex("[A-Z]+"))) {
            throw IllegalArgumentException("httpMethod must be an HTTP method token")
        }
        val normalizedMinimum = validStatus(minimumStatus, "minimumStatus")
        val normalizedMaximum = validStatus(maximumStatus, "maximumStatus")
        if (normalizedMinimum != null && normalizedMaximum != null && normalizedMinimum > normalizedMaximum) {
            throw IllegalArgumentException("minimumStatus must be <= maximumStatus")
        }
        val normalizedText =
            if (text == null || text.isBlank()) {
                null
            } else {
                text.trim()
            }
        this.apiId = apiId
        this.httpMethod = normalizedMethod
        this.minimumStatus = normalizedMinimum
        this.maximumStatus = normalizedMaximum
        this.createdAt = createdAt
        this.text = normalizedText
        this.page = required(page, "page")
    }

    fun getApiId(): Long? = apiId

    fun getHttpMethod(): String? = httpMethod

    fun getMinimumStatus(): Int? = minimumStatus

    fun getMaximumStatus(): Int? = maximumStatus

    fun getCreatedAt(): TimeRange? = createdAt

    fun getText(): String? = text

    fun getPage(): PageRequest = page

    override fun equals(other: Any?): Boolean =
        recordEquals(other) {
            apiId == it.apiId &&
                httpMethod == it.httpMethod &&
                minimumStatus == it.minimumStatus &&
                maximumStatus == it.maximumStatus &&
                createdAt == it.createdAt &&
                text == it.text &&
                page == it.page
        }

    override fun hashCode(): Int = recordHash(apiId, httpMethod, minimumStatus, maximumStatus, createdAt, text, page)

    override fun toString(): String =
        recordString(
            "RequestLogQuery",
            "apiId" to apiId,
            "httpMethod" to httpMethod,
            "minimumStatus" to minimumStatus,
            "maximumStatus" to maximumStatus,
            "createdAt" to createdAt,
            "text" to text,
            "page" to page,
        )

    companion object {
        private fun validStatus(
            status: Int?,
            name: String,
        ): Int? {
            if (status == null) {
                return null
            }
            if (status < 100 || status > 599) {
                throw IllegalArgumentException("$name must be between 100 and 599")
            }
            return status
        }
    }
}
