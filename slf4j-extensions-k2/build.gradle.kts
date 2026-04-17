plugins {
    kotlin("jvm")
}

dependencies {
    implementation(project(":slf4j-extensions-common"))
    compileOnly("org.jetbrains.kotlin:kotlin-compiler-embeddable")
}
