package com.apimetric.sample

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
import org.springframework.web.server.ResponseStatusException
import java.util.Objects

@RestController
@RequestMapping("/sample/items")
class SampleItemController {
    @GetMapping
    fun list(): List<Item> = listOf(Item("1", "alpha"))

    @GetMapping("/{id}")
    fun get(
        @PathVariable id: String,
    ): Item {
        if (id == "missing") {
            throw ResponseStatusException(HttpStatus.NOT_FOUND, "item was not found")
        }
        return Item(id, "alpha")
    }

    @PostMapping
    fun create(
        @RequestBody item: Item,
    ): ResponseEntity<Item> =
        ResponseEntity
            .status(HttpStatus.CREATED)
            .body(Item("2", if (item.name == null) "unnamed" else item.name))

    @PutMapping("/{id}")
    fun update(
        @PathVariable id: String,
        @RequestBody item: Item,
    ): Item = Item(id, item.name)

    @DeleteMapping("/{id}")
    fun delete(
        @PathVariable id: String?,
    ): ResponseEntity<Void> {
        Objects.requireNonNull(id, "id")
        return ResponseEntity.noContent().build()
    }

    data class Item(
        val id: String?,
        val name: String?,
    )
}
