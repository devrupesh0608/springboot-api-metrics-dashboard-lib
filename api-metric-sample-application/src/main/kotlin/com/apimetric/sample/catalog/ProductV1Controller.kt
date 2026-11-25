package com.apimetric.sample.catalog

import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

/** Version 1 product API: id, name, and price. */
@RestController
@RequestMapping("/api/v1/products")
class ProductV1Controller(
    private val catalog: ProductCatalog,
) {
    @GetMapping
    fun list(): List<ProductResponse> = catalog.list().map { product -> ProductResponse.from(product) }

    @GetMapping("/{id}")
    fun get(
        @PathVariable id: String,
    ): ProductResponse = ProductResponse.from(catalog.get(id))

    @PostMapping
    fun create(
        @RequestBody request: ProductRequest,
    ): ResponseEntity<ProductResponse> {
        val created = catalog.create(request.name, request.sku, request.price)
        return ResponseEntity.status(HttpStatus.CREATED).body(ProductResponse.from(created))
    }

    @PutMapping("/{id}")
    fun update(
        @PathVariable id: String,
        @RequestBody request: ProductRequest,
    ): ProductResponse = ProductResponse.from(catalog.update(id, request.name, request.sku, request.price))

    @DeleteMapping("/{id}")
    fun delete(
        @PathVariable id: String,
    ): ResponseEntity<Void> {
        catalog.delete(id)
        return ResponseEntity.noContent().build()
    }

    data class ProductRequest(
        val name: String?,
        val sku: String?,
        val price: Int,
    )

    data class ProductResponse(
        val id: String,
        val name: String,
        val price: Int,
    ) {
        companion object {
            fun from(product: Product): ProductResponse = ProductResponse(product.id, product.name, product.price)
        }
    }
}
