plugins {
    kotlin("jvm") version "2.3.20"
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
    implementation("org.slf4j:slf4j-simple:1.7.36")
}
