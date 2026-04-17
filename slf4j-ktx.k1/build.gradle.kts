plugins {
    kotlin("jvm")
}

description = "K1 frontend (descriptor-based) for the slf4j-ktx compiler plugin"

val kotlinVersion: String by project

dependencies {
    compileOnly("org.jetbrains.kotlin:kotlin-compiler-embeddable:$kotlinVersion")
    implementation(project(":slf4j-ktx.common"))
}

kotlin {
    compilerOptions {
        optIn.add("org.jetbrains.kotlin.compiler.plugin.ExperimentalCompilerApi")
    }
}
