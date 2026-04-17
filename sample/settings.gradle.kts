pluginManagement {
    repositories {
        mavenCentral()
        gradlePluginPortal()
    }
    includeBuild("..")
}

dependencyResolutionManagement {
    repositories {
        mavenCentral()
    }
}

// Composite build: included root project exposes its subprojects by their Gradle project.name.
// `:slf4j-ktx-core` and `:slf4j-ktx-compiler-plugin-embeddable` already match the consumer-side
// dependency coordinates, so no dependencySubstitution is needed — Gradle auto-matches
// `io.github.harryjhin:slf4j-ktx-core:*` and `io.github.harryjhin:slf4j-ktx-compiler-plugin-embeddable:*`.
includeBuild("..")

rootProject.name = "sample"
