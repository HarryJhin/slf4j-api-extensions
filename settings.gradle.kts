pluginManagement {
    val kotlinVersion: String by settings
    plugins {
        kotlin("jvm") version kotlinVersion
    }
}

plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "0.8.0"
}

rootProject.name = "slf4j-extensions"

include(":slf4j-extensions-common")
include(":slf4j-extensions-runtime")
include(":slf4j-extensions-k1")
include(":slf4j-extensions-k2")
include(":slf4j-extensions-backend")
include(":slf4j-extensions-cli")
include(":slf4j-extensions-compiler")
include(":slf4j-extensions-gradle-plugin")

// slf4j-ktx rewrite scaffolds (Step 2). Coexist with legacy modules until Step 14.
include(":slf4j-ktx.common")
include(":slf4j-ktx.k1")
include(":slf4j-ktx.k2")
include(":slf4j-ktx.backend")
include(":slf4j-ktx.cli")
include(":slf4j-ktx.embeddable")
include(":slf4j-ktx-core")
include(":slf4j-ktx-gradle-plugin")
include(":slf4j-ktx-spring-gradle-plugin")
