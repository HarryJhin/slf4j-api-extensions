plugins {
    kotlin("jvm") apply false
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
    project(":slf4j-extensions-common"),
    project(":slf4j-extensions-k1"),
    project(":slf4j-extensions-k2"),
    project(":slf4j-extensions-backend"),
    project(":slf4j-extensions-cli"),
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
