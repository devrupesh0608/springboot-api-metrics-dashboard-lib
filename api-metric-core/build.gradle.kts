plugins {
    `java-library`
    kotlin("jvm")
    id("org.jetbrains.dokka")
    id("com.vanniktech.maven.publish")
    id("io.spring.dependency-management")
    id("com.diffplug.spotless")
}

description = "Framework-neutral monitoring model and storage contracts."

dependencies {
    testImplementation("org.junit.jupiter:junit-jupiter")
    testImplementation("org.assertj:assertj-core")
}
