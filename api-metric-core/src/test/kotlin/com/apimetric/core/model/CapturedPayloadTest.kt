package com.apimetric.core.model

import com.apimetric.core.enums.CaptureStatus
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test

class CapturedPayloadTest {
    @Test
    fun capturedAndTruncatedPayloadsKeepBody() {
        val captured = CapturedPayload.captured("application/json", "{\"ok\":true}")
        val truncated = CapturedPayload.truncated("text/plain", "abc")

        assertThat(captured.status).isEqualTo(CaptureStatus.CAPTURED)
        assertThat(captured.body).isEqualTo("{\"ok\":true}")
        assertThat(truncated.status).isEqualTo(CaptureStatus.TRUNCATED)
        assertThat(truncated.body).isEqualTo("abc")
    }

    @Test
    fun skippedPayloadsCannotCarryABody() {
        assertThat(CapturedPayload.notCaptured().body).isNull()
        assertThat(CapturedPayload.excluded("multipart/form-data").body).isNull()
        assertThat(CapturedPayload.unsupported("application/octet-stream").body).isNull()
        assertThatThrownBy { CapturedPayload(CaptureStatus.EXCLUDED, "application/json", "{\"a\":1}") }
            .isInstanceOf(IllegalArgumentException::class.java)
            .hasMessageContaining("body must be absent")
        assertThatThrownBy { CapturedPayload(CaptureStatus.CAPTURED, "text/plain", null) }
            .isInstanceOf(IllegalArgumentException::class.java)
            .hasMessageContaining("body is required")
    }
}
