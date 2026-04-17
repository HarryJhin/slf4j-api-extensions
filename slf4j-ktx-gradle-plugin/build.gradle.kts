plugins {
    kotlin("jvm")
    `java-gradle-plugin`
}

description = "Gradle plugin that applies slf4j-ktx compiler plugin and exposes the slf4jKtx { } DSL"

val kotlinVersion: String by project

dependencies {
    compileOnly("org.jetbrains.kotlin:kotlin-gradle-plugin-api:$kotlinVersion")
}

// Stamp the plugin version into a classpath resource so the runtime can pass it explicitly to
// SubpluginArtifact(group, artifact, version). Without an explicit version the Kotlin Gradle
// plugin falls back to the consumer's Kotlin version — which breaks SNAPSHOT consumption
// (user on stable Kotlin 1.9.25 cannot resolve our 1.9.25-SNAPSHOT compiler artifact).
val generatePluginVersionProperties = tasks.register("generatePluginVersionProperties") {
    val outputDir = layout.buildDirectory.dir("generated/resources/version")
    val pluginVersion = project.version.toString()
    inputs.property("version", pluginVersion)
    outputs.dir(outputDir)
    doLast {
        val file = outputDir.get().file("io/github/harryjhin/slf4j/ktx/gradle/version.properties").asFile
        file.parentFile.mkdirs()
        file.writeText("version=$pluginVersion\n")
    }
}

sourceSets.main {
    resources.srcDir(generatePluginVersionProperties)
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
