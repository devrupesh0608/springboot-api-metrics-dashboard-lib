package com.apimetric.autoconfigure.capture

import com.apimetric.autoconfigure.ApiMetricProperties
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.util.AntPathMatcher
import org.springframework.web.servlet.HandlerInterceptor
import org.springframework.web.servlet.HandlerMapping
import org.springframework.web.util.pattern.PathPattern

/** Copies the resolved route template onto the request after Spring MVC matches a handler. */
class ApiMetricRouteInterceptor(
    private val properties: ApiMetricProperties,
) : HandlerInterceptor {
    private val matcher = AntPathMatcher()

    override fun afterCompletion(
        request: HttpServletRequest,
        response: HttpServletResponse,
        handler: Any,
        ex: Exception?,
    ) {
        val pattern = request.getAttribute(HandlerMapping.BEST_MATCHING_PATTERN_ATTRIBUTE)
        if (pattern is PathPattern) {
            request.setAttribute(ROUTE_ATTRIBUTE, pattern.patternString)
        } else if (pattern != null) {
            request.setAttribute(ROUTE_ATTRIBUTE, pattern.toString())
        }
    }

    fun isExcluded(path: String?): Boolean {
        if (path == null) {
            return true
        }
        val dashboard = normalize(properties.dashboard.path)
        val internal = normalize(properties.internalPath)
        if (path == dashboard || path.startsWith("$dashboard/")) {
            return true
        }
        if (path == internal || path.startsWith("$internal/")) {
            return true
        }
        for (pattern in properties.capture.excludedPaths) {
            if (pattern != null && matcher.match(pattern, path)) {
                return true
            }
        }
        return false
    }

    companion object {
        const val ROUTE_ATTRIBUTE = "com.apimetric.routePattern"

        private fun normalize(path: String?): String {
            if (path.isNullOrBlank()) {
                return ""
            }
            return if (path.endsWith("/") && path.length > 1) {
                path.substring(0, path.length - 1)
            } else {
                path
            }
        }
    }
}
