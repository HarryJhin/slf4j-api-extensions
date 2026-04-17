package io.github.harryjhin.slf4j.ktx.compiler

import org.jetbrains.kotlin.config.CompilerConfigurationKey

/**
 * Compiler-configuration keys consumed by the slf4j-ktx plugin. Intentionally minimal — the
 * plugin's only tunable is the set of trigger annotations. Everything else is expressed via
 * annotations on user code, matching kotlinx-serialization's policy (single CLI option, no
 * bag of knobs).
 */
object Slf4jKtxConfigurationKeys {
    /** Additional trigger-annotation FQNs supplied by the user via CLI / Gradle DSL. */
    val ANNOTATIONS: CompilerConfigurationKey<List<String>> =
        CompilerConfigurationKey.create("slf4j-ktx.annotations")
}
