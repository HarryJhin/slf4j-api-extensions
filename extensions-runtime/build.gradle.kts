plugins {
    kotlin("jvm")
    `java-library`
}

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

tasks.test {
    useJUnitPlatform()
}
