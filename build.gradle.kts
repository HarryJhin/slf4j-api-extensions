import org.jetbrains.dokka.DokkaConfiguration
import org.jetbrains.dokka.gradle.DokkaTask
import java.net.URL

plugins {
    kotlin("jvm") version "2.0.10"
    id("org.jetbrains.dokka") version "1.9.20"
    `maven-publish`
    id("org.jreleaser") version "1.13.1"
}

description = "SLF4J API extensions for Kotlin"
group = "io.github.harryjhin"
version = "1.0.3"

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(8)
        vendor = JvmVendorSpec.ADOPTIUM
    }
    withJavadocJar()
    withSourcesJar()
}

repositories {
    mavenCentral()
}

dependencies {
    implementation("org.slf4j:slf4j-api:2.0.16")
    testImplementation("org.slf4j:slf4j-simple:2.0.16")
    testImplementation(kotlin("test"))
}

tasks.test {
    useJUnitPlatform()
}

tasks.withType<DokkaTask>().configureEach {
    outputDirectory.set(file("docs"))

    dokkaSourceSets {
        configureEach {
            documentedVisibilities.set(
                setOf(
                    DokkaConfiguration.Visibility.PUBLIC,
                    DokkaConfiguration.Visibility.PROTECTED,
                )
            )
            includes.from(project.files())
            sourceLink {
                localDirectory.set(file("src/main/kotlin"))
                remoteUrl.set(URL("https://github.com/HarryJhin/slf4j-api-extensions/blob/master/src/main/kotlin"))
                remoteLineSuffix.set("#L")
            }
            samples.from(project.files(), "${rootDir}/src/test/kotlin")
        }
    }
}

tasks.register<Jar>("dokkaHtmlJar") {
    dependsOn(tasks.dokkaHtml)
    from(tasks.dokkaHtml.flatMap { it.outputDirectory })
    archiveClassifier.set("html-docs")
}

tasks.register<Jar>("dokkaJavadocJar") {
    dependsOn(tasks.dokkaJavadoc)
    from(tasks.dokkaJavadoc.flatMap { it.outputDirectory })
    archiveClassifier.set("javadoc")
}

tasks.jar {
    dependsOn(tasks.named("dokkaJavadocJar"))
}

publishing {
    publications {
        create<MavenPublication>("maven") {
            from(components["java"])

            pom {
                name.set(project.name)
                description.set(project.description)
                url.set("https://github.com/HarryJhin/slf4j-api-extensions")
                inceptionYear.set("2024")
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
                        email.set("joojinhyun00@gmail.com")
                    }
                }
                scm {
                    connection.set("scm:git://github.com/HarryJhin/slf4j-api-extensions.git")
                    developerConnection.set("scm:git:ssh://github.com/HarryJhin/slf4j-api-extensions.git")
                    url.set("https://github.com/HarryJhin/slf4j-api-extensions/tree/master")
                }
            }
        }
    }

    repositories {
        maven {
            setUrl(layout.buildDirectory.dir("publish"))
        }
    }
}

jreleaser {
    deploy {
        setActive("RELEASE")
        maven {
            setActive("RELEASE")
            pomchecker {
                version = "1.11.0"
                failOnWarning = true
                failOnError = true
            }
            mavenCentral {
                create("centralPortal") {
                    setActive("RELEASE")
                    url = "https://central.sonatype.com/api/v1/publisher"
                    stagingRepository(layout.buildDirectory.dir("publish").get().toString())
                    retryDelay = 5
                    maxRetries = 600
                }
            }
        }
    }
    release {
        github {
            enabled = true
            repoOwner = "HarryJhin"
            host = "github.com"
            overwrite = false
            update {
                enabled = false
            }
            commitAuthor {
                name = "주진현"
                email = "joojinhyun00@gmail.com"
            }
        }
    }
    signing {
        setActive("RELEASE")
        armored = true
        verify = true
    }
}