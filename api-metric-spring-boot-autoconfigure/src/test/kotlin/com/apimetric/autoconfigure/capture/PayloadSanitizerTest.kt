package com.apimetric.autoconfigure.capture

import com.apimetric.core.enums.CaptureStatus
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.nio.charset.StandardCharsets

class PayloadSanitizerTest {
    private val sanitizer =
        PayloadSanitizer(
            10240,
            listOf("password", "accessToken"),
            listOf("multipart/form-data"),
        )

    @Test
    fun masksNestedJsonFieldsWithoutCaseSensitivity() {
        val body =
            """
            {"user":"ada","Password":"secret","nested":{"accessToken":"abc"}}
            """.trim().toByteArray(StandardCharsets.UTF_8)

        val payload = sanitizer.sanitize(true, "application/json", body)

        assertThat(payload.status).isEqualTo(CaptureStatus.CAPTURED)
        assertThat(payload.body).contains("***").doesNotContain("secret").doesNotContain("abc")
        assertThat(payload.body).contains("ada")
    }

    @Test
    fun marksOversizedPayloadsTruncated() {
        val body = "x".repeat(40).toByteArray(StandardCharsets.UTF_8)

        val limited = PayloadSanitizer(32, listOf(), listOf())
        val payload = limited.sanitize(true, "text/plain", body)

        assertThat(payload.status).isEqualTo(CaptureStatus.TRUNCATED)
        assertThat(payload.body).hasSize(32)
    }

    @Test
    fun masksHeaderNamesAndCookie() {
        val headerSanitizer = PayloadSanitizer(10240, listOf("authorization"), listOf("multipart/form-data"))
        val headers =
            headerSanitizer.sanitizeHeaders(
                true,
                mapOf(
                    "Authorization" to listOf("Bearer secret"),
                    "Cookie" to listOf("session=abc"),
                    "Accept" to listOf("application/json"),
                ),
            )

        assertThat(headers).contains("***").contains("application/json")
        assertThat(headers).doesNotContain("Bearer").doesNotContain("session")
    }

    @Test
    fun skipsMultipartBodies() {
        val body = "file".toByteArray(StandardCharsets.UTF_8)

        val payload = sanitizer.sanitize(true, "multipart/form-data; boundary=abc", body)

        assertThat(payload.status).isEqualTo(CaptureStatus.UNSUPPORTED)
        assertThat(payload.body).isNull()
    }
}
