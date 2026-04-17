plugins {
    kotlin("jvm")
    `java-gradle-plugin`
}

dependencies {
    compileOnly("org.jetbrains.kotlin:kotlin-gradle-plugin-api")
}

// Embed plugin version into a properties file so the runtime plugin class can
// report it to SubpluginArtifact. Without an explicit version, the Kotlin
// Gradle plugin falls back to the *Kotlin* plugin version, which breaks when
// a consumer on stable Kotlin consumes our SNAPSHOT plugin (version mismatch).
val generatePluginVersionProperties = tasks.register("generatePluginVersionProperties") {
    val outputDir = layout.buildDirectory.dir("generated/resources/version")
    val pluginVersion = project.version.toString()
    inputs.property("version", pluginVersion)
    outputs.dir(outputDir)
    doLast {
        val file = outputDir.get().file("io/github/harryjhin/slf4j/extensions/gradle/version.properties").asFile
        file.parentFile.mkdirs()
        file.writeText("version=$pluginVersion\n")
    }
}

sourceSets.main {
    resources.srcDir(generatePluginVersionProperties)
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
