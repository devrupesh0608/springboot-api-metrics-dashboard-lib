package com.apimetric.core.internal

import java.time.Duration
import java.util.Objects

/** Validation helpers for the core model. Not a supported extension point. */
object Checks {
    @JvmStatic
    fun <T : Any> required(
        value: T?,
        name: String,
    ): T {
        @Suppress("UNCHECKED_CAST")
        return Objects.requireNonNull(value, name) as T
    }

    @JvmStatic
    fun requiredText(
        value: String?,
        name: String,
    ): String {
        val text = required(value, name)
        val trimmed = text.trim()
        if (trimmed.isEmpty()) {
            throw IllegalArgumentException("$name is required")
        }
        return trimmed
    }

    @JvmStatic
    fun blankToNull(value: String?): String? {
        if (value == null) {
            return null
        }
        val trimmed = value.trim()
        return if (trimmed.isEmpty()) null else trimmed
    }

    @JvmStatic
    fun positiveDuration(
        value: Duration?,
        name: String,
    ): Duration {
        val duration = required(value, name)
        if (duration.isZero || duration.isNegative) {
            throw IllegalArgumentException("$name must be positive")
        }
        return duration
    }

    @JvmStatic
    fun nonNegative(
        value: Long,
        name: String,
    ): Long {
        if (value < 0) {
            throw IllegalArgumentException("$name must be >= 0")
        }
        return value
    }

    @JvmStatic
    fun positive(
        value: Long,
        name: String,
    ): Long {
        if (value <= 0) {
            throw IllegalArgumentException("$name must be positive")
        }
        return value
    }

    @JvmStatic
    fun requireHttpStatus(status: Int): Int {
        if (status < 100 || status > 599) {
            throw IllegalArgumentException("httpStatus must be between 100 and 599")
        }
        return status
    }

    @JvmStatic
    fun <T> immutableList(
        values: List<T>?,
        name: String,
    ): List<T> = java.util.List.copyOf(required(values, name))
}

/** Java record hash: start at 0, then `result = 31 * result + componentHash` for each component. */
internal fun recordHash(vararg values: Any?): Int {
    var result = 0
    for (value in values) {
        result = 31 * result + (value?.hashCode() ?: 0)
    }
    return result
}

/** Java record equality: identity, then the same runtime class, then component equality. */
internal inline fun <reified T : Any> T.recordEquals(
    other: Any?,
    sameComponents: (T) -> Boolean,
): Boolean {
    if (this === other) {
        return true
    }
    if (other !is T) {
        return false
    }
    return sameComponents(other)
}

/** Java record `toString`: `Type[name=value, name=value]`. */
internal fun recordString(
    typeName: String,
    vararg components: Pair<String, Any?>,
): String {
    val body = components.joinToString(", ") { (name, value) -> "$name=$value" }
    return "$typeName[$body]"
}
