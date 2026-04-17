package io.github.harryjhin.slf4j.ktx.compiler

import org.jetbrains.kotlin.name.FqName

/**
 * String constants that must stay identical across the plugin (compiler side), the Gradle
 * subplugin (user side), and the runtime. Any drift between these sites breaks the -Xplugin
 * wiring, the CLI option name, or the annotation the plugin listens for.
 */
object Slf4jKtxPluginNames {
    /** Compiler-plugin id. Also the Gradle-plugin id. */
    const val PLUGIN_ID: String = "io.github.harryjhin.slf4j-ktx"

    /** Single CLI option — accepts one annotation FQN per occurrence; multi-valued. */
    const val ANNOTATION_OPTION: String = "annotation"

    /**
     * Name of the `Logger` property the plugin synthesizes on the generation site (Companion
     * of a triggered class, or the triggered `object` itself). Also the symbol the IR pass
     * looks for when rewriting runtime-extension call sites.
     */
    const val LOG_PROPERTY_NAME: String = "log"

    /** The hard-coded trigger: the annotation shipped in slf4j-ktx-core. */
    val SLF4J_ANNOTATION_FQ_NAME: FqName = FqName("io.github.harryjhin.slf4j.ktx.Slf4j")
}
