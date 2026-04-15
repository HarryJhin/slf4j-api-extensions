plugins {
    kotlin("jvm") version "2.3.20" apply false
}

group = "io.github.harryjhin"

subprojects {
    group = rootProject.group
    version = rootProject.version

    repositories {
        mavenCentral()
    }
}

// Compiler plugin modules need opt-in for internal APIs
configure(listOf(
    project(":extensions-common"),
    project(":extensions-k1"),
    project(":extensions-k2"),
    project(":extensions-backend"),
    project(":extensions-cli"),
)) {
    pluginManager.withPlugin("org.jetbrains.kotlin.jvm") {
        extensions.configure<org.jetbrains.kotlin.gradle.dsl.KotlinJvmProjectExtension> {
            compilerOptions {
                optIn.add("org.jetbrains.kotlin.compiler.plugin.ExperimentalCompilerApi")
                optIn.add("org.jetbrains.kotlin.ir.symbols.UnsafeDuringIrConstructionAPI")
            }
        }
    }
}
