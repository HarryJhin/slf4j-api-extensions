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

// Publishing configuration for distributable modules
configure(listOf(
    project(":slf4j-extensions-runtime"),
    project(":slf4j-extensions-compiler"),
    project(":slf4j-extensions-gradle-plugin"),
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
                create<MavenPublication>("maven") {
                    from(components["java"])
                    pom {
                        name.set(project.name)
                        description.set("Kotlin compiler plugin that injects SLF4J Logger into classes")
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
            }
            repositories {
                maven {
                    name = "sonatype"
                    url = uri("https://ossrh-staging-api.central.sonatype.com/service/local/staging/deploy/maven2/")
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
                sign(publishing.publications["maven"])
            }
        }
    }
}

// Compiler plugin modules need opt-in for internal APIs
configure(listOf(
    project(":slf4j-extensions-common"),
    project(":slf4j-extensions-k1"),
    project(":slf4j-extensions-backend"),
    project(":slf4j-extensions-cli"),
)) {
    pluginManager.withPlugin("org.jetbrains.kotlin.jvm") {
        extensions.configure<org.jetbrains.kotlin.gradle.dsl.KotlinJvmProjectExtension> {
            compilerOptions {
                optIn.add("org.jetbrains.kotlin.compiler.plugin.ExperimentalCompilerApi")
            }
        }
    }
}
