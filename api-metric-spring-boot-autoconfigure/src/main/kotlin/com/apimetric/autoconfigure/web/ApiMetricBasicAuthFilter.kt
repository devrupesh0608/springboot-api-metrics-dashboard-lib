package com.apimetric.autoconfigure.web

import com.apimetric.autoconfigure.ApiMetricProperties
import jakarta.servlet.Filter
import jakarta.servlet.FilterChain
import jakarta.servlet.ServletRequest
import jakarta.servlet.ServletResponse
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import java.util.Base64

/**
 * Asks for a username and password on the dashboard and its JSON API.
 * Business routes are not registered with this filter.
 */
class ApiMetricBasicAuthFilter(
    private val properties: ApiMetricProperties,
) : Filter {
    override fun doFilter(
        request: ServletRequest,
        response: ServletResponse,
        chain: FilterChain,
    ) {
        if (!properties.security.requiresLogin()) {
            chain.doFilter(request, response)
            return
        }
        val header = (request as HttpServletRequest).getHeader(AUTHORIZATION)
        val parsed = readBasicCredentials(header)
        val username = parsed?.first ?: ""
        val password = parsed?.second ?: ""
        val matches = credentialsMatch(username, password) && parsed != null
        if (matches) {
            chain.doFilter(request, response)
            return
        }
        val httpResponse = response as HttpServletResponse
        httpResponse.setHeader(WWW_AUTHENTICATE, CHALLENGE)
        httpResponse.status = HttpServletResponse.SC_UNAUTHORIZED
        httpResponse.contentType = "text/plain;charset=UTF-8"
        httpResponse.writer.write("Authentication required")
    }

    private fun credentialsMatch(
        username: String,
        password: String,
    ): Boolean {
        val userMatches = sameSecret(properties.security.username.orEmpty(), username)
        val passwordMatches = sameSecret(properties.security.password.orEmpty(), password)
        return userMatches && passwordMatches
    }

    companion object {
        private const val AUTHORIZATION = "Authorization"
        private const val WWW_AUTHENTICATE = "WWW-Authenticate"
        private const val CHALLENGE = "Basic realm=\"API Metrics Dashboard\", charset=\"UTF-8\""

        fun urlPatterns(
            dashboardPath: String,
            internalPath: String,
        ): List<String> {
            val patterns = linkedSetOf<String>()
            addPathFamily(patterns, dashboardPath)
            addPathFamily(patterns, internalPath)
            return patterns.toList()
        }

        private fun addPathFamily(
            patterns: MutableSet<String>,
            rawPath: String,
        ) {
            val trimmed = rawPath.trim()
            if (trimmed.isEmpty() || trimmed == "/") {
                return
            }
            val withSlash = if (trimmed.startsWith("/")) trimmed else "/$trimmed"
            val base = withSlash.trimEnd('/')
            if (base.isEmpty()) {
                return
            }
            patterns.add(base)
            patterns.add("$base/")
            patterns.add("$base/*")
        }

        private fun readBasicCredentials(header: String?): Pair<String, String>? {
            if (header == null || !header.regionMatches(0, "Basic ", 0, 6, ignoreCase = true)) {
                return null
            }
            val token = header.substring(6).trim()
            val decoded =
                try {
                    String(Base64.getDecoder().decode(token), StandardCharsets.ISO_8859_1)
                } catch (_: IllegalArgumentException) {
                    return null
                }
            val separator = decoded.indexOf(':')
            if (separator < 0) {
                return null
            }
            return decoded.substring(0, separator) to decoded.substring(separator + 1)
        }

        private fun sameSecret(
            expected: String,
            actual: String,
        ): Boolean {
            val digest = MessageDigest.getInstance("SHA-256")
            val expectedHash = digest.digest(expected.toByteArray(StandardCharsets.UTF_8))
            val actualHash = digest.digest(actual.toByteArray(StandardCharsets.UTF_8))
            return MessageDigest.isEqual(expectedHash, actualHash)
        }
    }
}
