package com.apimetric.core.model

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test

class ApiIdentityTest {
    @Test
    fun normalizesMethodAndBuildsStableKey() {
        val identity = ApiIdentity.of(" get ", "/users/{id}")

        assertThat(identity.httpMethod).isEqualTo("GET")
        assertThat(identity.pathPattern).isEqualTo("/users/{id}")
        assertThat(identity.apiKey()).isEqualTo("GET /users/{id}")
        assertThat(ApiIdentity.fromApiKey(identity.apiKey())).isEqualTo(identity)
    }

    @Test
    fun preservesDistinctRouteTemplates() {
        val collection = ApiIdentity.of("GET", "/users")
        val oneUser = ApiIdentity.of("GET", "/users/{id}")

        assertThat(collection.apiKey()).isEqualTo("GET /users")
        assertThat(oneUser.apiKey()).isEqualTo("GET /users/{id}")
        assertThat(collection).isNotEqualTo(oneUser)
    }

    @Test
    fun rejectsQueryStringFragmentUrlAndUnrootedPath() {
        assertThatThrownBy { ApiIdentity.of("GET", "/users?token=secret") }
            .isInstanceOf(IllegalArgumentException::class.java)
            .hasMessageContaining("query string")
        assertThatThrownBy { ApiIdentity.of("GET", "/users#section") }
            .isInstanceOf(IllegalArgumentException::class.java)
            .hasMessageContaining("fragment")
        assertThatThrownBy { ApiIdentity.of("GET", "http://localhost/users") }
            .isInstanceOf(IllegalArgumentException::class.java)
            .hasMessageContaining("not a URL")
        assertThatThrownBy { ApiIdentity.of("GET", "users/{id}") }
            .isInstanceOf(IllegalArgumentException::class.java)
            .hasMessageContaining("start with /")
        assertThatThrownBy { ApiIdentity.of("GET", "/users /{id}") }
            .isInstanceOf(IllegalArgumentException::class.java)
            .hasMessageContaining("whitespace")
    }

    @Test
    fun rejectsMalformedMethodAndApiKey() {
        assertThatThrownBy { ApiIdentity.of(" ", "/users") }
            .isInstanceOf(IllegalArgumentException::class.java)
            .hasMessageContaining("httpMethod")
        assertThatThrownBy { ApiIdentity.of("G ET", "/users") }
            .isInstanceOf(IllegalArgumentException::class.java)
            .hasMessageContaining("HTTP method token")
        assertThatThrownBy { ApiIdentity.fromApiKey("GET") }
            .isInstanceOf(IllegalArgumentException::class.java)
            .hasMessageContaining("METHOD SPACE path")
        assertThatThrownBy { ApiIdentity.fromApiKey("GET /users?x=1") }
            .isInstanceOf(IllegalArgumentException::class.java)
            .hasMessageContaining("query string")
    }

    @Test
    fun rejectsNullMethodPathAndKey() {
        assertThatThrownBy { ApiIdentity.of(null, "/users") }
            .isInstanceOf(NullPointerException::class.java)
            .hasMessage("httpMethod")
        assertThatThrownBy { ApiIdentity.of("GET", null) }
            .isInstanceOf(NullPointerException::class.java)
            .hasMessage("pathPattern")
        assertThatThrownBy { ApiIdentity.fromApiKey(null) }
            .isInstanceOf(NullPointerException::class.java)
            .hasMessage("apiKey")
    }
}
