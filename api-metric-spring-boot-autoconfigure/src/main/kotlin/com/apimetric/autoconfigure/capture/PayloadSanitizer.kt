package com.apimetric.autoconfigure.capture

import com.apimetric.core.model.CapturedPayload
import tools.jackson.core.JacksonException
import tools.jackson.databind.JsonNode
import tools.jackson.databind.json.JsonMapper
import tools.jackson.databind.node.ObjectNode
import java.io.StringReader
import java.nio.charset.Charset
import java.nio.charset.StandardCharsets
import java.util.Collections
import java.util.Locale
import kotlin.math.max
import kotlin.math.min

/** Size-limits and masks captured text. Masking is not a guarantee that every secret is found. */
class PayloadSanitizer(
    private val maxPayloadSize: Int,
    maskFields: Collection<String?>,
    excludedContentTypes: Collection<String?>,
) {
    private val mapper = JsonMapper.shared()
    private val maskFields: Set<String> =
        Collections.unmodifiableSet(
            maskFields
                .filterNotNull()
                .filter { field -> field.isNotBlank() }
                .map { field -> field.lowercase(Locale.ROOT) }
                .toSet(),
        )
    private val excludedContentTypes: Set<String> =
        Collections.unmodifiableSet(
            excludedContentTypes
                .filterNotNull()
                .filter { type -> type.isNotBlank() }
                .map { type -> type.lowercase(Locale.ROOT) }
                .toSet(),
        )

    fun sanitize(
        captureEnabled: Boolean,
        contentType: String?,
        body: ByteArray?,
    ): CapturedPayload {
        if (!captureEnabled) {
            return CapturedPayload.notCaptured()
        }
        if (body == null || body.isEmpty()) {
            return CapturedPayload.notCaptured()
        }
        val normalizedType = normalizeType(contentType)
        if (isUnsupported(normalizedType)) {
            return CapturedPayload.unsupported(contentType)
        }
        if (isExcluded(normalizedType)) {
            return CapturedPayload.excluded(contentType)
        }
        val truncated = body.size > maxPayloadSize
        val length = min(body.size, max(maxPayloadSize, 0))
        var text = String(body, 0, length, charset(contentType))
        if (isJson(normalizedType)) {
            text = maskJson(text)
        }
        if (truncated) {
            return CapturedPayload.truncated(contentType, text)
        }
        return CapturedPayload.captured(contentType, text)
    }

    /**
     * Returns masked headers as JSON, or null when header capture is off. Values for configured mask
     * names, plus Cookie and Set-Cookie, are replaced. The JSON is cut at the payload size limit.
     */
    fun sanitizeHeaders(
        captureEnabled: Boolean,
        headers: Map<String, List<String>>?,
    ): String? {
        if (!captureEnabled || headers.isNullOrEmpty()) {
            return null
        }
        val node = mapper.createObjectNode()
        for ((name, headerValues) in headers) {
            if (name.isBlank()) {
                continue
            }
            val value = if (maskHeader(name)) MASK else headerValues.joinToString(", ")
            node.put(name, value)
        }
        if (node.isEmpty()) {
            return null
        }
        val json = mapper.writeValueAsString(node)
        if (json.length > maxPayloadSize) {
            return json.substring(0, max(maxPayloadSize, 0))
        }
        return json
    }

    private fun maskHeader(name: String): Boolean {
        val lower = name.lowercase(Locale.ROOT)
        return lower == "cookie" || lower == "set-cookie" || maskFields.contains(lower)
    }

    private fun maskJson(text: String): String {
        try {
            val tree = mapper.readTree(StringReader(text))
            mask(tree)
            return mapper.writeValueAsString(tree)
        } catch (ex: JacksonException) {
            return text
        }
    }

    private fun mask(node: JsonNode?) {
        if (node == null || node.isNull) {
            return
        }
        if (node.isObject) {
            val objectNode = node as ObjectNode
            for (name in objectNode.propertyNames().toList()) {
                if (maskFields.contains(name.lowercase(Locale.ROOT))) {
                    objectNode.put(name, MASK)
                } else {
                    mask(objectNode.get(name))
                }
            }
            return
        }
        if (node.isArray) {
            node.forEach { child -> mask(child) }
        }
    }

    private fun isExcluded(contentType: String): Boolean = excludedContentTypes.any { excluded -> contentType.startsWith(excluded) }

    private fun charset(contentType: String?): Charset {
        if (contentType == null) {
            return StandardCharsets.UTF_8
        }
        val lower = contentType.lowercase(Locale.ROOT)
        val marker = lower.indexOf("charset=")
        if (marker < 0) {
            return StandardCharsets.UTF_8
        }
        var charsetName = contentType.substring(marker + "charset=".length).trim()
        val end = charsetName.indexOf(';')
        if (end >= 0) {
            charsetName = charsetName.substring(0, end).trim()
        }
        return try {
            Charset.forName(charsetName)
        } catch (ex: Exception) {
            StandardCharsets.UTF_8
        }
    }

    companion object {
        private const val MASK = "***"

        private fun isUnsupported(contentType: String): Boolean =
            contentType.startsWith("multipart/") ||
                contentType.startsWith("application/octet-stream") ||
                contentType.startsWith("image/") ||
                contentType.startsWith("video/") ||
                contentType.startsWith("audio/")

        private fun isJson(contentType: String): Boolean = contentType.contains("json") || contentType.contains("+json")

        private fun normalizeType(contentType: String?): String {
            if (contentType.isNullOrBlank()) {
                return ""
            }
            val separator = contentType.indexOf(';')
            val type = if (separator >= 0) contentType.substring(0, separator) else contentType
            return type.trim().lowercase(Locale.ROOT)
        }
    }
}
