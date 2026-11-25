plugins {
    `java-library`
    kotlin("jvm")
    kotlin("plugin.spring")
    id("org.jetbrains.dokka")
    id("com.vanniktech.maven.publish")
    id("io.spring.dependency-management")
    id("com.diffplug.spotless")
}

description = "Spring Boot auto-configuration for API monitoring."

dependencies {
    api(project(":api-metric-core"))
    api(project(":api-metric-storage-mysql"))
    // Optional in the published POM so the host Spring Boot 4.2 BOM supplies this jar.
    api("org.springframework.boot:spring-boot-autoconfigure")
    implementation("tools.jackson.core:jackson-databind")
    implementation("org.slf4j:slf4j-api")
    // Provided by the host Servlet application. compileOnly so this jar does not pull a web stack.
    compileOnly("org.springframework:spring-webmvc")
    compileOnly("jakarta.servlet:jakarta.servlet-api")

    testImplementation("org.junit.jupiter:junit-jupiter")
    testImplementation("org.assertj:assertj-core")
}
