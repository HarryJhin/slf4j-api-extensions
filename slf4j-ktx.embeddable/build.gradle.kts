plugins {
    kotlin("jvm")
}

description = "Fat JAR of the slf4j-ktx compiler plugin for end-user consumption"

// Same pattern as the legacy `slf4j-extensions-compiler` module — a custom, non-transitive
// configuration that we unpack into this module's jar task. Relocation is NOT needed: every
// internal module declares its Kotlin-compiler dependency as `compileOnly`, so compiler
// internals (intellij, asm, protobuf, etc.) are never pulled into this JAR in the first place.
// The user's Kotlin compiler supplies them at plugin load time.
val embedded by configurations.creating {
    isTransitive = false
}

dependencies {
    embedded(project(":slf4j-ktx.common"))
    embedded(project(":slf4j-ktx.k1"))
    embedded(project(":slf4j-ktx.k2"))
    embedded(project(":slf4j-ktx.backend"))
    embedded(project(":slf4j-ktx.cli"))
}

tasks.jar {
    dependsOn(embedded)
    from({
        embedded.map { if (it.isDirectory) it else zipTree(it) }
    })
    // Only cli contributes META-INF/services; others contribute disjoint class paths.
    // EXCLUDE is safe — nothing overlaps meaningfully across internal modules.
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
}
