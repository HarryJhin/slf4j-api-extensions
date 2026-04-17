plugins {
    kotlin("jvm")
}

val embedded by configurations.creating {
    isTransitive = false
}

dependencies {
    embedded(project(":slf4j-extensions-common"))
    embedded(project(":slf4j-extensions-k1"))
    embedded(project(":slf4j-extensions-k2"))
    embedded(project(":slf4j-extensions-backend"))
    embedded(project(":slf4j-extensions-cli"))
}

tasks.jar {
    dependsOn(embedded)
    from({
        embedded.map { if (it.isDirectory) it else zipTree(it) }
    })
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
}
