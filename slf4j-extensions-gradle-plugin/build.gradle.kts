plugins {
    kotlin("jvm")
    `java-gradle-plugin`
    id("com.gradle.plugin-publish") version "1.3.0"
}

dependencies {
    compileOnly("org.jetbrains.kotlin:kotlin-gradle-plugin-api")
}

gradlePlugin {
    website = "https://github.com/HarryJhin/slf4j-api-extensions"
    vcsUrl = "https://github.com/HarryJhin/slf4j-api-extensions"

    plugins {
        create("slf4jExtensions") {
            id = "io.github.harryjhin.slf4j-extensions"
            displayName = "SLF4J Extensions Kotlin Compiler Plugin"
            description = "Kotlin compiler plugin that injects SLF4J Logger into classes"
            implementationClass = "io.github.harryjhin.slf4j.extensions.gradle.Slf4jExtensionsGradlePlugin"
            tags = listOf("kotlin", "slf4j", "logging", "compiler-plugin")
        }
    }
}
