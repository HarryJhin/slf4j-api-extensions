plugins {
    kotlin("jvm")
    `java-gradle-plugin`
}

description = "Gradle plugin that applies slf4j-ktx compiler plugin and exposes the slf4jKtx { } DSL"

val kotlinVersion: String by project

dependencies {
    compileOnly("org.jetbrains.kotlin:kotlin-gradle-plugin-api:$kotlinVersion")
    compileOnly("org.jetbrains.kotlin:kotlin-gradle-plugin:$kotlinVersion")
}

// Plugin descriptor wired up in Step 10.
