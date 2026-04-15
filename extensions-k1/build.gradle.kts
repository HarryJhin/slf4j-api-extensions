plugins {
    kotlin("jvm")
}

dependencies {
    implementation(project(":extensions-common"))
    compileOnly("org.jetbrains.kotlin:kotlin-compiler-embeddable")
}
