plugins {
    kotlin("jvm")
}

description = "IR lowering for the slf4j-ktx compiler plugin (Companion member body generation)"

val kotlinVersion: String by project

dependencies {
    compileOnly("org.jetbrains.kotlin:kotlin-compiler-embeddable:$kotlinVersion")
    implementation(project(":slf4j-ktx.common"))
    implementation(project(":slf4j-ktx.k1"))
    implementation(project(":slf4j-ktx.k2"))
}

kotlin {
    compilerOptions {
        optIn.add("org.jetbrains.kotlin.compiler.plugin.ExperimentalCompilerApi")
    }
}
