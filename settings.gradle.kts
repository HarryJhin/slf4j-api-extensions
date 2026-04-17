pluginManagement {
    val kotlinVersion: String by settings
    plugins {
        kotlin("jvm") version kotlinVersion
    }
}

plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "0.8.0"
}

rootProject.name = "slf4j-ktx"

// Compiler-plugin modules (internal, not published)
include(":slf4j-ktx.common")
include(":slf4j-ktx.k1")
include(":slf4j-ktx.k2")
include(":slf4j-ktx.backend")
include(":slf4j-ktx.cli")
// Embeddable module: directory keeps the dot-notation name for symmetry with serialization's
// internal module layout, but Gradle path + project name are hyphenated so composite-build
// consumers can resolve `io.github.harryjhin:slf4j-ktx-compiler-plugin-embeddable:*` by
// auto-matching (no dependencySubstitution needed).
include(":slf4j-ktx-compiler-plugin-embeddable")
project(":slf4j-ktx-compiler-plugin-embeddable").projectDir = file("slf4j-ktx.embeddable")

// Published modules
include(":slf4j-ktx-core")
include(":slf4j-ktx-gradle-plugin")
include(":slf4j-ktx-spring-gradle-plugin")
