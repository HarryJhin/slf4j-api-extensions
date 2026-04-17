plugins {
    kotlin("jvm")
    `java-gradle-plugin`
}

description = "Gradle plugin that applies slf4j-ktx compiler plugin and exposes the slf4jKtx { } DSL"

val kotlinVersion: String by project

dependencies {
    compileOnly("org.jetbrains.kotlin:kotlin-gradle-plugin-api:$kotlinVersion")
}

gradlePlugin {
    plugins {
        create("slf4jKtx") {
            id = "io.github.harryjhin.slf4j-ktx"
            displayName = "slf4j-ktx Kotlin compiler plugin"
            description = "Kotlin compiler plugin that injects SLF4J Logger into annotated classes"
            implementationClass = "io.github.harryjhin.slf4j.ktx.gradle.Slf4jKtxGradleSubplugin"
        }
    }
}
