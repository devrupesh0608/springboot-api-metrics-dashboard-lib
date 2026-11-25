package com.apimetric.autoconfigure.web

import com.apimetric.autoconfigure.ApiMetricProperties
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.server.ResponseStatusException
import java.nio.charset.StandardCharsets

/** Serves the request inspector from the host application. */
@RestController
@RequestMapping("\${api-metric.dashboard.path:/api-metric-dashboard}")
class ApiMetricDashboardController(
    private val properties: ApiMetricProperties,
) {
    @GetMapping(value = ["", "/"], produces = [MediaType.TEXT_HTML_VALUE])
    fun page(): ResponseEntity<String> {
        if (!properties.dashboard.enabled) {
            throw ResponseStatusException(HttpStatus.NOT_FOUND)
        }
        if (properties.security.enabled && !properties.security.allowUnauthenticated) {
            throw ResponseStatusException(HttpStatus.NOT_FOUND)
        }
        return ResponseEntity.ok(html(properties.internalPath))
    }

    private fun html(internal: String): String {
        val scriptPath = internal.replace("\\", "\\\\").replace("\"", "\\\"")
        val template =
            ApiMetricDashboardController::class.java
                .getResourceAsStream("/api-metric-dashboard.html")
                ?.bufferedReader(StandardCharsets.UTF_8)
                ?.use { reader -> reader.readText() }
                ?: throw IllegalStateException("api-metric-dashboard.html is missing from the classpath")
        return template.replace("{{internal}}", scriptPath)
    }
}
