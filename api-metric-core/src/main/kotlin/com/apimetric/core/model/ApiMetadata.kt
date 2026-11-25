package com.apimetric.core.model

import com.apimetric.core.internal.Checks.blankToNull
import com.apimetric.core.internal.Checks.required
import com.apimetric.core.internal.Checks.requiredText
import com.apimetric.core.internal.recordEquals
import com.apimetric.core.internal.recordHash
import com.apimetric.core.internal.recordString

/** A route observed from the application's handler mappings. */
class ApiMetadata(
    identity: ApiIdentity?,
    controllerName: String?,
    handlerMethod: String?,
    displayName: String?,
) {
    @get:JvmName("identity")
    val identity: ApiIdentity

    @get:JvmName("controllerName")
    val controllerName: String

    @get:JvmName("handlerMethod")
    val handlerMethod: String

    @get:JvmName("displayName")
    val displayName: String?

    init {
        this.identity = required(identity, "identity")
        this.controllerName = requiredText(controllerName, "controllerName")
        this.handlerMethod = requiredText(handlerMethod, "handlerMethod")
        this.displayName = blankToNull(displayName)
    }

    fun getIdentity(): ApiIdentity = identity

    fun getControllerName(): String = controllerName

    fun getHandlerMethod(): String = handlerMethod

    fun getDisplayName(): String? = displayName

    override fun equals(other: Any?): Boolean =
        recordEquals(other) {
            identity == it.identity &&
                controllerName == it.controllerName &&
                handlerMethod == it.handlerMethod &&
                displayName == it.displayName
        }

    override fun hashCode(): Int = recordHash(identity, controllerName, handlerMethod, displayName)

    override fun toString(): String =
        recordString(
            "ApiMetadata",
            "identity" to identity,
            "controllerName" to controllerName,
            "handlerMethod" to handlerMethod,
            "displayName" to displayName,
        )
}
