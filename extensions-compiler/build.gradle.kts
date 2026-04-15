plugins {
    kotlin("jvm")
}

val embedded by configurations.creating {
    isTransitive = false
}

dependencies {
    embedded(project(":extensions-common"))
    embedded(project(":extensions-k1"))
    embedded(project(":extensions-k2"))
    embedded(project(":extensions-backend"))
    embedded(project(":extensions-cli"))
}

tasks.jar {
    dependsOn(embedded)
    from({
        embedded.map { if (it.isDirectory) it else zipTree(it) }
    })
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
}
