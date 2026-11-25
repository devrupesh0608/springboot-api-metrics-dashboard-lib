package com.apimetric.sample

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class SampleApplication {
    companion object {
        @JvmStatic
        fun main(args: Array<String>) {
            runApplication<SampleApplication>(*args)
        }
    }
}
