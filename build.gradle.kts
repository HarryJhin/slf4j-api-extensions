plugins {
    kotlin("jvm") apply false
    `maven-publish`
    signing
}

group = "io.github.harryjhin"

subprojects {
    group = rootProject.group
    version = rootProject.version

    repositories {
        mavenCentral()
    }
}

// Publishing + signing for the four end-user artifacts.
// Each module carries its own build.gradle.kts; this block wires common maven-publish / signing
// conventions. Individual modules may override artifactId (notably slf4j-ktx.embeddable, whose
// Gradle path contains a dot).
configure(listOf(
    project(":slf4j-ktx-core"),
    project(":slf4j-ktx-compiler-plugin-embeddable"),
    project(":slf4j-ktx-gradle-plugin"),
    project(":slf4j-ktx-spring-gradle-plugin"),
)) {
    apply(plugin = "maven-publish")
    apply(plugin = "signing")

    pluginManager.withPlugin("java") {
        extensions.configure<JavaPluginExtension> {
            withSourcesJar()
            withJavadocJar()
        }
    }

    afterEvaluate {
        publishing {
            publications {
                // `java-gradle-plugin` already creates `pluginMaven` + plugin-marker publications
                // for Gradle plugin projects. Only add our own `maven` publication for non-plugin
                // modules; adding both would publish duplicate coordinates to the same repo.
                if (!project.pluginManager.hasPlugin("java-gradle-plugin")) {
                    create<MavenPublication>("maven") {
                        from(components["java"])
                    }
                }
            }
            // POM metadata applied to every MavenPublication on the project
            // (our `maven` for non-plugin modules, `pluginMaven` + plugin markers for plugin modules).
            publications.withType<MavenPublication>().configureEach {
                pom {
                    name.set(artifactId)
                    description.set(project.description ?: "Kotlin compiler plugin that injects SLF4J Logger into classes")
                    url.set("https://github.com/HarryJhin/slf4j-api-extensions")
                    licenses {
                        license {
                            name.set("MIT License")
                            url.set("https://opensource.org/licenses/MIT")
                        }
                    }
                    developers {
                        developer {
                            id.set("HarryJhin")
                            name.set("주진현")
                        }
                    }
                    scm {
                        url.set("https://github.com/HarryJhin/slf4j-api-extensions")
                    }
                }
            }
            repositories {
                maven {
                    name = "sonatype"
                    val isSnapshot = version.toString().endsWith("-SNAPSHOT")
                    url = uri(
                        if (isSnapshot) "https://central.sonatype.com/repository/maven-snapshots/"
                        else "https://ossrh-staging-api.central.sonatype.com/service/local/staging/deploy/maven2/"
                    )
                    credentials {
                        username = findProperty("mavenCentralUsername")?.toString()
                        password = findProperty("mavenCentralPassword")?.toString()
                    }
                }
            }
        }

        signing {
            val signingRequired = !version.toString().contains("-SNAPSHOT")
            isRequired = signingRequired
            if (signingRequired) {
                val key = findProperty("signingInMemoryKey")?.toString()
                val password = findProperty("signingInMemoryKeyPassword")?.toString()
                if (!key.isNullOrBlank()) {
                    useInMemoryPgpKeys(key, password)
                } else {
                    useGpgCmd()
                }
                sign(publishing.publications)
            }
        }
    }
}

// Compiler-plugin-facing modules need ExperimentalCompilerApi opt-in. Each module also declares
// this locally, but keeping the root configure block in place matches the pattern from the legacy
// layout and documents the intent.
configure(listOf(
    project(":slf4j-ktx.common"),
    project(":slf4j-ktx.k1"),
    project(":slf4j-ktx.k2"),
    project(":slf4j-ktx.backend"),
    project(":slf4j-ktx.cli"),
)) {
    pluginManager.withPlugin("org.jetbrains.kotlin.jvm") {
        extensions.configure<org.jetbrains.kotlin.gradle.dsl.KotlinJvmProjectExtension> {
            compilerOptions {
                optIn.add("org.jetbrains.kotlin.compiler.plugin.ExperimentalCompilerApi")
            }
        }
    }
}
