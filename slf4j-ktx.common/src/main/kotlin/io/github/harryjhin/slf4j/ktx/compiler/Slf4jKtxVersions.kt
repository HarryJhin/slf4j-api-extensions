package io.github.harryjhin.slf4j.ktx.compiler

import java.io.File
import java.util.jar.Attributes
import java.util.jar.JarFile

/**
 * Version-compatibility primitives shared by K1 and K2 paths.
 *
 * - [Version] + parser.
 * - [RuntimeVersions] snapshot matching the two manifest attributes
 *   (`Implementation-Version` + `Require-Kotlin-Version`) stamped by `slf4j-ktx-core`.
 * - [MINIMAL_SUPPORTED_VERSION] — the ABI floor the current plugin build supports.
 * - [readFromJar] — manifest reader, platform-independent.
 *
 * Frontend-specific lookup (K1 via ModuleDescriptor, K2 via FirSession/SymbolProvider) lives in
 * the respective modules.
 */
object Slf4jKtxVersions {

    data class Version(val major: Int, val minor: Int, val patch: Int) : Comparable<Version> {
        override fun compareTo(other: Version): Int =
            compareValuesBy(this, other, { it.major }, { it.minor }, { it.patch })

        override fun toString(): String = "$major.$minor.$patch"

        companion object {
            /** Accepts forms like "1.9.25", "0.1.0-SNAPSHOT"; takes the leading numeric triple. */
            fun parse(raw: String): Version? {
                val parts = raw.split('.', '-').mapNotNull { it.toIntOrNull() }
                if (parts.size < 3) return null
                return Version(parts[0], parts[1], parts[2])
            }
        }
    }

    data class RuntimeVersions(
        val implementationVersion: Version?,
        val requireKotlinVersion: Version?,
    ) {
        fun implementationVersionMatchSupported(): Boolean =
            implementationVersion != null && implementationVersion >= MINIMAL_SUPPORTED_VERSION

        fun currentCompilerMatchRequired(currentCompiler: Version): Boolean =
            requireKotlinVersion == null || requireKotlinVersion <= currentCompiler
    }

    /**
     * ABI floor of `slf4j-ktx-core` this plugin build supports. Independent of plugin/Kotlin
     * version pair. Bump only when the plugin starts emitting calls to symbols that did not
     * exist in older core releases.
     */
    val MINIMAL_SUPPORTED_VERSION: Version = Version(0, 1, 0)

    const val REQUIRE_KOTLIN_ATTRIBUTE: String = "Require-Kotlin-Version"

    /** Reads both compatibility attributes from [file]. File must be a readable JAR. */
    fun readFromJar(file: File): RuntimeVersions =
        JarFile(file).use { jar ->
            val attrs = jar.manifest?.mainAttributes
            val implRaw = attrs?.getValue(Attributes.Name.IMPLEMENTATION_VERSION)
            val requireRaw = attrs?.getValue(REQUIRE_KOTLIN_ATTRIBUTE)
            RuntimeVersions(
                implementationVersion = implRaw?.let(Version::parse),
                requireKotlinVersion = requireRaw?.let(Version::parse),
            )
        }
}
