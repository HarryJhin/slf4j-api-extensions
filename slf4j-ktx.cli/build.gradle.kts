plugins {
    kotlin("jvm")
}

description = "CompilerPluginRegistrar + CommandLineProcessor aggregator for slf4j-ktx"

val kotlinVersion: String by project

dependencies {
    compileOnly("org.jetbrains.kotlin:kotlin-compiler-embeddable:$kotlinVersion")
    implementation(project(":slf4j-ktx.common"))
    implementation(project(":slf4j-ktx.k1"))
    implementation(project(":slf4j-ktx.k2"))
    implementation(project(":slf4j-ktx.backend"))

    // ----- box-test infrastructure (Step 12) -----
    testImplementation(project(":slf4j-ktx-core"))
    testImplementation("org.jetbrains.kotlin:kotlin-compiler:$kotlinVersion")
    testImplementation("org.jetbrains.kotlin:kotlin-compiler-internal-test-framework:$kotlinVersion")
    testImplementation(kotlin("test"))
    testImplementation(platform("org.junit:junit-bom:5.10.1"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testImplementation("org.junit.platform:junit-platform-commons")
    testImplementation("org.junit.platform:junit-platform-launcher")
    testImplementation("org.junit.platform:junit-platform-runner")
    testImplementation("org.junit.platform:junit-platform-suite-api")

    testRuntimeOnly("org.jetbrains.kotlin:kotlin-reflect:$kotlinVersion")
    testRuntimeOnly("org.jetbrains.kotlin:kotlin-script-runtime:$kotlinVersion")
    testRuntimeOnly("org.jetbrains.kotlin:kotlin-annotations-jvm:$kotlinVersion")
}

kotlin {
    compilerOptions {
        optIn.add("org.jetbrains.kotlin.compiler.plugin.ExperimentalCompilerApi")
    }
}

// Root-level testFixtures + tests-gen wired as additional test source directories
// (mirrors plugins/kotlinx-serialization/testFixtures + tests-gen layout where the plugin's
// own root project carries the test runner sources).
sourceSets {
    test {
        java.srcDirs(
            rootProject.file("testFixtures/kotlin"),
            rootProject.file("tests-gen"),
        )
    }
}

tasks.test {
    useJUnitPlatform()
    // workingDir = repository root so @TestMetadata relative paths like "testData/box/..." resolve.
    workingDir = rootDir
    testLogging {
        showStandardStreams = true
    }

    // kotlin-compiler-internal-test-framework reads stdlib/reflect/etc. locations from system
    // properties. Resolve them from this module's testRuntimeClasspath at task execution time.
    doFirst {
        val testClasspath = project.configurations.getByName("testRuntimeClasspath").files
        fun findJar(module: String): String {
            return testClasspath
                .find { it.name.matches(Regex("kotlin-${Regex.escape(module)}-\\d+\\..*\\.jar")) }
                ?.absolutePath
                ?: testClasspath.find { it.name == "kotlin-$module.jar" }?.absolutePath
                ?: ""
        }
        systemProperty("org.jetbrains.kotlin.test.kotlin-stdlib", findJar("stdlib"))
        systemProperty("org.jetbrains.kotlin.test.kotlin-stdlib-jdk8", findJar("stdlib-jdk8"))
        systemProperty("org.jetbrains.kotlin.test.kotlin-reflect", findJar("reflect"))
        systemProperty("org.jetbrains.kotlin.test.kotlin-test", findJar("test"))
        systemProperty("org.jetbrains.kotlin.test.kotlin-script-runtime", findJar("script-runtime"))
        systemProperty("org.jetbrains.kotlin.test.kotlin-annotations-jvm", findJar("stdlib"))
    }

    systemProperty("idea.ignore.disabled.plugins", "true")
    systemProperty("idea.home.path", rootDir)
}
