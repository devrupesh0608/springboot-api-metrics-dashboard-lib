package com.apimetric.autoconfigure.capture

import com.apimetric.autoconfigure.ApiMetricProperties
import com.apimetric.core.model.ApiIdentity
import com.apimetric.core.model.ApiRequestEvent
import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.slf4j.LoggerFactory
import org.springframework.web.filter.OncePerRequestFilter
import org.springframework.web.util.ContentCachingRequestWrapper
import org.springframework.web.util.ContentCachingResponseWrapper
import java.time.Instant
import java.util.ArrayList
import java.util.Enumeration
import java.util.LinkedHashMap
import java.util.concurrent.TimeUnit
import kotlin.math.max

/**
 * Captures a completed MVC request after the business chain returns. Monitoring failures are logged
 * without the payload and do not change the business response.
 */
class ApiMetricCaptureFilter(
    private val properties: ApiMetricProperties,
    private val routes: ApiMetricRouteInterceptor,
    private val queue: ApiMetricEventQueue,
    private val sanitizer: PayloadSanitizer,
) : OncePerRequestFilter() {
    override fun shouldNotFilter(request: HttpServletRequest): Boolean = routes.isExcluded(request.requestURI)

    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain,
    ) {
        val limit = max(properties.capture.maxPayloadSize, 1)
        val requestWrapper = ContentCachingRequestWrapper(request, limit)
        val responseWrapper = ContentCachingResponseWrapper(response)
        val started = System.nanoTime()
        try {
            filterChain.doFilter(requestWrapper, responseWrapper)
        } finally {
            try {
                publish(requestWrapper, responseWrapper, started)
            } catch (ex: RuntimeException) {
                log.warn("API Metric capture failed: {}", ex.javaClass.simpleName)
            }
            responseWrapper.copyBodyToResponse()
        }
    }

    private fun publish(
        request: ContentCachingRequestWrapper,
        response: ContentCachingResponseWrapper,
        started: Long,
    ) {
        if (request.isAsyncStarted) {
            return
        }
        val route = request.getAttribute(ApiMetricRouteInterceptor.ROUTE_ATTRIBUTE)
        if (route !is String || !route.startsWith("/")) {
            return
        }
        val identity =
            try {
                ApiIdentity.of(request.method, route)
            } catch (ex: RuntimeException) {
                return
            }
        val duration = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - started)
        val requestPayload =
            sanitizer.sanitize(
                properties.capture.requestPayload,
                request.contentType,
                request.contentAsByteArray,
            )
        val responsePayload =
            sanitizer.sanitize(
                properties.capture.responsePayload,
                response.contentType,
                response.contentAsByteArray,
            )
        val captureHeaders = properties.capture.captureHeaders
        queue.publish(
            ApiRequestEvent(
                identity,
                normalizeStatus(response.status),
                duration,
                correlationId(request),
                requestPayload,
                responsePayload,
                sanitizer.sanitizeHeaders(captureHeaders, requestHeaders(request)),
                sanitizer.sanitizeHeaders(captureHeaders, responseHeaders(response)),
                Instant.now(),
            ),
        )
    }

    companion object {
        private val log = LoggerFactory.getLogger(ApiMetricCaptureFilter::class.java)

        private fun requestHeaders(request: HttpServletRequest): Map<String, List<String>> {
            val headers = LinkedHashMap<String, List<String>>()
            val names = request.headerNames ?: return headers
            while (names.hasMoreElements()) {
                val name = names.nextElement()
                headers[name] = headerValues(request.getHeaders(name))
            }
            return headers
        }

        private fun responseHeaders(response: HttpServletResponse): Map<String, List<String>> {
            val headers = LinkedHashMap<String, List<String>>()
            for (name in response.headerNames) {
                headers[name] = ArrayList(response.getHeaders(name))
            }
            return headers
        }

        private fun headerValues(values: Enumeration<String>?): List<String> {
            val collected = ArrayList<String>()
            if (values == null) {
                return collected
            }
            while (values.hasMoreElements()) {
                collected.add(values.nextElement())
            }
            return collected
        }

        private fun normalizeStatus(status: Int): Int = if (status in 100..599) status else 500

        private fun correlationId(request: HttpServletRequest): String? {
            var id = request.getHeader("X-Correlation-Id")
            if (id == null || id.isBlank()) {
                id = request.getHeader("X-Request-Id")
            }
            return id
        }
    }
}
