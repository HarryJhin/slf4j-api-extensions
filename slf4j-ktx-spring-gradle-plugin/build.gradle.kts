plugins {
    kotlin("jvm")
    `java-gradle-plugin`
}

description = "Spring integration plugin that auto-applies slf4j-ktx and adds Spring stereotype triggers"

val kotlinVersion: String by project

dependencies {
    compileOnly("org.jetbrains.kotlin:kotlin-gradle-plugin-api:$kotlinVersion")
    compileOnly("org.jetbrains.kotlin:kotlin-gradle-plugin:$kotlinVersion")
    implementation(project(":slf4j-ktx-gradle-plugin"))
}

// Plugin descriptor wired up in Step 11.
