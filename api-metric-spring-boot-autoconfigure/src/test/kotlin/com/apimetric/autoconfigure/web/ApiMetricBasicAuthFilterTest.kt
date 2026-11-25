package com.apimetric.autoconfigure.web

import com.apimetric.autoconfigure.ApiMetricProperties
import jakarta.servlet.FilterChain
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import org.springframework.mock.web.MockHttpServletRequest
import org.springframework.mock.web.MockHttpServletResponse
import java.nio.charset.StandardCharsets
import java.util.Base64

class ApiMetricBasicAuthFilterTest {
    @Test
    fun missingAuthorizationChallengesTheBrowser() {
        val response = filter(loginProperties())

        assertThat(response.status).isEqualTo(401)
        assertThat(response.getHeader("WWW-Authenticate")).contains("Basic realm=\"API Metrics Dashboard\"")
        assertThat(response.contentAsString).doesNotContain("secret-pass")
    }

    @Test
    fun wrongPasswordIsRejected() {
        val response = filter(loginProperties(), basic("admin", "wrong"))

        assertThat(response.status).isEqualTo(401)
        assertThat(response.getHeader("WWW-Authenticate")).startsWith("Basic ")
    }

    @Test
    fun correctPasswordContinuesTheRequest() {
        val response = filter(loginProperties(), basic("admin", "secret-pass"))

        assertThat(response.status).isEqualTo(200)
        assertThat(response.getHeader("WWW-Authenticate")).isNull()
        assertThat(response.contentAsString).isEqualTo("ok")
    }

    @Test
    fun openDashboardDoesNotAskForCredentials() {
        val properties = ApiMetricProperties()
        properties.security.allowUnauthenticated = true
        properties.security.username = "admin"
        properties.security.password = "secret-pass"

        val response = filter(properties)

        assertThat(response.status).isEqualTo(200)
        assertThat(response.contentAsString).isEqualTo("ok")
    }

    @Test
    fun halfConfiguredLoginFailsStartup() {
        val properties = ApiMetricProperties()
        properties.storage.mysql.url = "jdbc:mysql://localhost/api_metrics"
        properties.storage.mysql.username = "api_metrics"
        properties.storage.mysql.password = "db-secret"
        properties.security.username = "admin"

        assertThatThrownBy { properties.validate() }
            .isInstanceOf(IllegalStateException::class.java)
            .hasMessageContaining("username")
            .hasMessageContaining("password")
    }

    @Test
    fun filterPatternsCoverDashboardAndInternalApiOnly() {
        assertThat(ApiMetricBasicAuthFilter.urlPatterns("/api-metric-dashboard", "/api-metric/internal"))
            .contains("/api-metric-dashboard", "/api-metric/internal/*")
            .doesNotContain("/*")
    }

    private fun loginProperties(): ApiMetricProperties {
        val properties = ApiMetricProperties()
        properties.security.username = "admin"
        properties.security.password = "secret-pass"
        return properties
    }

    private fun filter(
        properties: ApiMetricProperties,
        authorization: String? = null,
    ): MockHttpServletResponse {
        val request = MockHttpServletRequest("GET", "/api-metric-dashboard")
        if (authorization != null) {
            request.addHeader("Authorization", authorization)
        }
        val response = MockHttpServletResponse()
        var continued = false
        val chain =
            FilterChain { _, servletResponse ->
                continued = true
                servletResponse.writer.write("ok")
            }
        ApiMetricBasicAuthFilter(properties).doFilter(request, response, chain)
        if (!continued) {
            assertThat(response.status).isEqualTo(401)
        }
        return response
    }

    private fun basic(
        username: String,
        password: String,
    ): String {
        val token =
            Base64.getEncoder().encodeToString(
                "$username:$password".toByteArray(StandardCharsets.ISO_8859_1),
            )
        return "Basic $token"
    }
}
