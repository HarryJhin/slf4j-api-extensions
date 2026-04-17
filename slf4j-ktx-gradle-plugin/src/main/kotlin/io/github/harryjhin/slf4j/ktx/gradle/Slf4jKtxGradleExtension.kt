package io.github.harryjhin.slf4j.ktx.gradle

/**
 * DSL entry for the slf4j-ktx Gradle plugin. Mirrors [AllOpenExtension](https://github.com/JetBrains/kotlin/blob/master/libraries/tools/kotlin-allopen/src/common/kotlin/org/jetbrains/kotlin/allopen/gradle/AllOpenExtension.kt):
 * an eager `mutableListOf<String>` backing a single `annotation(fqName)` method.
 *
 * Usage:
 * ```kotlin
 * slf4jKtx {
 *     annotation("com.example.LoggedDomain")
 * }
 * ```
 */
open class Slf4jKtxGradleExtension {
    internal val myAnnotations: MutableList<String> = mutableListOf()

    open fun annotation(fqName: String) {
        myAnnotations.add(fqName)
    }
}
