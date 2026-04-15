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
