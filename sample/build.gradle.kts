plugins {
    kotlin("jvm") version "1.9.25"
    id("io.github.harryjhin.slf4j-extensions")
    application
}

application {
    mainClass.set("com.example.MainKt")
}

repositories {
    mavenCentral()
}

dependencies {
    implementation("io.github.harryjhin:slf4j-extensions-runtime:1.9.25-SNAPSHOT")
    implementation("org.slf4j:slf4j-simple:1.7.36")
}
