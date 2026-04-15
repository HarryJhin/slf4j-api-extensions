plugins {
    kotlin("jvm")
    `java-gradle-plugin`
}

dependencies {
    compileOnly("org.jetbrains.kotlin:kotlin-gradle-plugin-api")
}

gradlePlugin {
    plugins {
        create("slf4jExtensions") {
            id = "io.github.harryjhin.slf4j-extensions"
            displayName = "SLF4J Extensions Kotlin Compiler Plugin"
            description = "Kotlin compiler plugin that injects SLF4J Logger into classes"
            implementationClass = "io.github.harryjhin.slf4j.extensions.gradle.Slf4jExtensionsGradlePlugin"
        }
    }
}
