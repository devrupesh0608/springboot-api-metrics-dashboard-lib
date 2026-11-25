package com.apimetric.core.model

import com.apimetric.core.internal.Checks.required
import com.apimetric.core.internal.recordEquals
import com.apimetric.core.internal.recordHash
import com.apimetric.core.internal.recordString
import java.util.Locale
import java.util.regex.Pattern

/**
 * Stable identity of one API: an HTTP method plus a route template.
 *
 * The key is `METHOD /path/{var}`. Raw request URLs, query strings, and numeric ids are not
 * identities.
 */
class ApiIdentity(
    httpMethod: String?,
    pathPattern: String?,
) {
    @get:JvmName("httpMethod")
    val httpMethod: String

    @get:JvmName("pathPattern")
    val pathPattern: String

    init {
        val normalizedMethod = required(httpMethod, "httpMethod").trim().uppercase(Locale.ROOT)
        val normalizedPath = required(pathPattern, "pathPattern").trim()
        if (normalizedMethod.isEmpty()) {
            throw IllegalArgumentException("httpMethod is required")
        }
        if (!METHOD.matcher(normalizedMethod).matches()) {
            throw IllegalArgumentException("httpMethod must be an HTTP method token")
        }
        if (normalizedPath.indexOf('?') >= 0) {
            throw IllegalArgumentException("pathPattern must be a route template without a query string")
        }
        if (normalizedPath.indexOf('#') >= 0) {
            throw IllegalArgumentException("pathPattern must be a route template without a fragment")
        }
        if (containsWhitespace(normalizedPath)) {
            throw IllegalArgumentException("pathPattern must not contain whitespace")
        }
        if (normalizedPath.contains("://")) {
            throw IllegalArgumentException("pathPattern must be a route template, not a URL")
        }
        if (!normalizedPath.startsWith("/")) {
            throw IllegalArgumentException("pathPattern must start with /")
        }
        this.httpMethod = normalizedMethod
        this.pathPattern = normalizedPath
    }

    fun getHttpMethod(): String = httpMethod

    fun getPathPattern(): String = pathPattern

    fun apiKey(): String = "$httpMethod $pathPattern"

    override fun equals(other: Any?): Boolean =
        recordEquals(other) {
            httpMethod == it.httpMethod && pathPattern == it.pathPattern
        }

    override fun hashCode(): Int = recordHash(httpMethod, pathPattern)

    override fun toString(): String = recordString("ApiIdentity", "httpMethod" to httpMethod, "pathPattern" to pathPattern)

    companion object {
        private val METHOD: Pattern = Pattern.compile("[A-Z][A-Z0-9]*")

        @JvmStatic
        fun of(
            httpMethod: String?,
            pathPattern: String?,
        ): ApiIdentity = ApiIdentity(httpMethod, pathPattern)

        /** Parses a key produced by [apiKey]. */
        @JvmStatic
        fun fromApiKey(apiKey: String?): ApiIdentity {
            val key = required(apiKey, "apiKey")
            val separator = key.indexOf(' ')
            if (separator <= 0 || separator != key.lastIndexOf(' ')) {
                throw IllegalArgumentException("apiKey must be METHOD SPACE path")
            }
            return ApiIdentity(key.substring(0, separator), key.substring(separator + 1))
        }

        private fun containsWhitespace(value: String): Boolean = value.codePoints().anyMatch { Character.isWhitespace(it) }
    }
}
