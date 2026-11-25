plugins {
    `java-library`
    kotlin("jvm")
    id("org.jetbrains.dokka")
    id("com.vanniktech.maven.publish")
    id("io.spring.dependency-management")
    id("com.diffplug.spotless")
}

description = "Dependency-only starter for API monitoring. Contains no application code."

dependencies {
    api(project(":api-metric-spring-boot-autoconfigure"))
    api(project(":api-metric-storage-mysql"))
    runtimeOnly("com.mysql:mysql-connector-j")
}
