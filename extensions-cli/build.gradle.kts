plugins {
    kotlin("jvm")
}

dependencies {
    implementation(project(":extensions-common"))
    implementation(project(":extensions-k1"))
    implementation(project(":extensions-k2"))
    implementation(project(":extensions-backend"))
    compileOnly("org.jetbrains.kotlin:kotlin-compiler-embeddable")

    testImplementation(project(":extensions-runtime"))
    testImplementation("org.jetbrains.kotlin:kotlin-compiler-embeddable")
    testImplementation("dev.zacsweers.kctfork:core:0.7.0")
    testImplementation(kotlin("test"))
}

tasks.test {
    useJUnitPlatform()
}
