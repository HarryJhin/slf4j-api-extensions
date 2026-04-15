plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "0.8.0"
}

rootProject.name = "slf4j-extensions"

include(":extensions-common")
include(":extensions-runtime")
include(":extensions-k1")
include(":extensions-k2")
include(":extensions-backend")
include(":extensions-cli")
include(":extensions-compiler")
