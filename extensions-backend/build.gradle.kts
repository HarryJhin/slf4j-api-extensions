plugins {
    kotlin("jvm")
}

kotlin {
    compilerOptions {
        optIn.add("org.jetbrains.kotlin.compiler.plugin.ExperimentalCompilerApi")
        optIn.add("org.jetbrains.kotlin.ir.symbols.UnsafeDuringIrConstructionAPI")
    }
}

dependencies {
    implementation(project(":extensions-common"))
    compileOnly("org.jetbrains.kotlin:kotlin-compiler-embeddable")
}
