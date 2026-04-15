plugins {
    kotlin("jvm")
}

dependencies {
    implementation(project(":extensions-common"))
    implementation(project(":extensions-k1"))
    implementation(project(":extensions-k2"))
    implementation(project(":extensions-backend"))
    implementation(project(":extensions-cli"))
}
