plugins {
    kotlin("jvm")
    `java-gradle-plugin`
    `java-library`
}

description = "Spring integration plugin that auto-applies slf4j-ktx and adds Spring stereotype triggers"

val kotlinVersion: String by project

dependencies {
    // Needed for Kotlin to resolve Slf4jKtxGradleSubplugin's supertype
    // (KotlinCompilerPluginSupportPlugin) during `plugins.apply(...::class.java)` overload resolution.
    compileOnly("org.jetbrains.kotlin:kotlin-gradle-plugin-api:$kotlinVersion")
    // `api` so consumers that depend on the Spring plugin (e.g. build-logic convention plugins)
    // can reference Slf4jKtxGradleExtension directly to add custom trigger annotations without
    // declaring a separate dependency on the main gradle plugin. Mirrors allopen+spring's
    // `commonApi` wiring in JetBrains/kotlin (libraries/tools/kotlin-allopen/build.gradle.kts).
    api(project(":slf4j-ktx-gradle-plugin"))
}

gradlePlugin {
    plugins {
        create("slf4jKtxSpring") {
            id = "io.github.harryjhin.slf4j-ktx.spring"
            displayName = "slf4j-ktx Spring integration"
            description = "Auto-applies slf4j-ktx and triggers on Spring stereotypes (@Component, @Controller, @Service, @Repository, @RestController, @ControllerAdvice)"
            implementationClass = "io.github.harryjhin.slf4j.ktx.spring.gradle.Slf4jKtxSpringGradleSubplugin"
        }
    }
}
