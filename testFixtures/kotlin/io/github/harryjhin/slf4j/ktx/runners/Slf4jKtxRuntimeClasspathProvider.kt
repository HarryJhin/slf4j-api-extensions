package io.github.harryjhin.slf4j.ktx.runners

import org.jetbrains.kotlin.test.model.TestModule
import org.jetbrains.kotlin.test.services.RuntimeClasspathProvider
import org.jetbrains.kotlin.test.services.TestServices
import java.io.File

/**
 * Supplies the `fun box()` execution classpath with the slf4j-api JAR and the slf4j-ktx-core
 * JAR (the latter carrying the `@Slf4j` trigger annotation). Both JARs are located by probing
 * the test JVM's own class loader — Gradle ensures they are on testRuntimeClasspath.
 */
class Slf4jKtxRuntimeClasspathProvider(testServices: TestServices) : RuntimeClasspathProvider(testServices) {
    override fun runtimeClassPaths(module: TestModule): List<File> =
        listOfNotNull(
            findJarContaining("org.slf4j.Logger"),
            findJarContaining("io.github.harryjhin.slf4j.ktx.Slf4j"),
        )
}

/**
 * Locates the JAR file containing a given fully-qualified class name via the current class
 * loader's resource lookup. Returns null when the class is not on the classpath (which is a
 * legitimate configuration for runtime-version diagnostic tests — to be added in later steps).
 */
internal fun findJarContaining(className: String): File? {
    val resource = className.replace('.', '/') + ".class"
    val url = Slf4jKtxRuntimeClasspathProvider::class.java.classLoader.getResource(resource) ?: return null
    val s = url.toString()
    if (!s.startsWith("jar:file:")) return null
    return File(s.removePrefix("jar:file:").substringBefore("!"))
}
