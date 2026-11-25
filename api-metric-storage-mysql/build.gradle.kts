plugins {
    `java-library`
    kotlin("jvm")
    id("org.jetbrains.dokka")
    id("com.vanniktech.maven.publish")
    id("io.spring.dependency-management")
    id("com.diffplug.spotless")
}

description = "Dedicated MySQL pool, schema, and repositories for API monitoring."

dependencies {
    api(project(":api-metric-core"))
    // Direct JDBC and pool libraries. Do not add spring-boot-starter-jdbc or
    // spring-boot-starter-flyway; those configure the host application's datasource.
    implementation("org.springframework:spring-jdbc")
    implementation("com.zaxxer:HikariCP")
    implementation("org.flywaydb:flyway-core")
    implementation("org.flywaydb:flyway-mysql")
    runtimeOnly("com.mysql:mysql-connector-j")
}
