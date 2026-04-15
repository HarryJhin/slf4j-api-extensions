plugins {
    kotlin("jvm") version "2.0.10" apply false
}

group = "io.github.harryjhin"

subprojects {
    group = rootProject.group
    version = rootProject.version
}
