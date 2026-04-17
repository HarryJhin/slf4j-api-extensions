plugins {
    kotlin("jvm")
    `java-library`
}

description = "Runtime library for slf4j-ktx: @Slf4j annotation + Marker/MDC inline extensions"

// Independent cadence — slf4j-ktx-core is versioned separately from the compiler plugin.
// The two axes of compatibility (coreVersion + requireKotlin) are stamped into MANIFEST.MF
// so the plugin's VersionReader can read them from the user's classpath at compile time.
val coreVersion: String by project
val requireKotlin: String by project

version = coreVersion

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(8)
    }
}

dependencies {
    api("org.slf4j:slf4j-api:1.7.36")

    // slf4j-test 3.0.x brings slf4j-api 2.0.x transitively for tests
    testImplementation("com.github.valfirst:slf4j-test:3.0.3")
    testImplementation(kotlin("test"))
}

tasks.jar {
    manifest {
        attributes(
            "Implementation-Title" to "slf4j-ktx-core",
            "Implementation-Version" to coreVersion,
            "Require-Kotlin-Version" to requireKotlin,
        )
    }
}

tasks.test {
    useJUnitPlatform()
}
