package com.apimetric.sample

import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.server.ResponseStatusException

@RestController
@RequestMapping("/sample/orders")
class SampleOrderController {
    @PostMapping
    fun create(
        @RequestBody request: OrderRequest,
    ): Map<String, Any> {
        val sku = request.sku
        if (sku.isNullOrBlank()) {
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, "sku is required")
        }
        return java.util.Map.of("id", "1001", "sku", sku, "quantity", request.quantity)
    }

    data class OrderRequest(
        val sku: String?,
        val quantity: Int,
        val password: String?,
        val note: String?,
    )
}
