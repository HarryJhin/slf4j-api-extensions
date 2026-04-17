plugins {
    kotlin("jvm") version "1.9.25"
    id("io.github.harryjhin.slf4j-ktx")
    application
}

application {
    mainClass.set("com.example.MainKt")
}

repositories {
    mavenCentral()
}

dependencies {
    implementation("io.github.harryjhin:slf4j-ktx-core:0.1.0-SNAPSHOT")
    implementation("org.slf4j:slf4j-simple:1.7.36")
}
