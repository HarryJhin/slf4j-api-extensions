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
    testImplementation("org.jetbrains.kotlin:kotlin-compiler:2.3.20")
    testImplementation("org.jetbrains.kotlin:kotlin-compiler-internal-test-framework:2.3.20")
    testImplementation(kotlin("test"))
    testImplementation(platform("org.junit:junit-bom:5.10.1"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testImplementation("org.junit.platform:junit-platform-commons")
    testImplementation("org.junit.platform:junit-platform-launcher")
    testImplementation("org.junit.platform:junit-platform-runner")
    testImplementation("org.junit.platform:junit-platform-suite-api")

    testRuntimeOnly("org.jetbrains.kotlin:kotlin-reflect:2.3.20")
    testRuntimeOnly("org.jetbrains.kotlin:kotlin-script-runtime:2.3.20")
    testRuntimeOnly("org.jetbrains.kotlin:kotlin-annotations-jvm:2.3.20")
}

tasks.test {
    useJUnitPlatform()
    workingDir = rootDir
    testLogging {
        showStandardStreams = true
    }

    doFirst {
        val testClasspath = project.configurations.getByName("testRuntimeClasspath").files
        fun findJar(module: String): String {
            // Match exact module name: kotlin-{module}-{version}.jar
            return testClasspath
                .find { it.name.matches(Regex("kotlin-${Regex.escape(module)}-\\d+\\..*\\.jar")) }
                ?.absolutePath
                ?: testClasspath
                    .find { it.name == "kotlin-$module.jar" }
                    ?.absolutePath
                ?: ""
        }
        // Debug: print resolved paths
        listOf("stdlib", "stdlib-jdk8", "reflect", "test", "script-runtime").forEach { module ->
            val path = findJar(module)
            println("  $module -> $path")
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
