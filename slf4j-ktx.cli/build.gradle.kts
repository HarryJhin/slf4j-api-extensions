plugins {
    kotlin("jvm")
}

description = "CompilerPluginRegistrar + CommandLineProcessor aggregator for slf4j-ktx"

val kotlinVersion: String by project

dependencies {
    compileOnly("org.jetbrains.kotlin:kotlin-compiler-embeddable:$kotlinVersion")
    implementation(project(":slf4j-ktx.common"))
    implementation(project(":slf4j-ktx.k1"))
    implementation(project(":slf4j-ktx.k2"))
    implementation(project(":slf4j-ktx.backend"))
}

kotlin {
    compilerOptions {
        optIn.add("org.jetbrains.kotlin.compiler.plugin.ExperimentalCompilerApi")
    }
}
