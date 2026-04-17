package io.github.harryjhin.slf4j.ktx.compiler.k1

import org.jetbrains.kotlin.diagnostics.rendering.CommonRenderers
import org.jetbrains.kotlin.diagnostics.rendering.DefaultErrorMessages
import org.jetbrains.kotlin.diagnostics.rendering.DiagnosticFactoryToRendererMap

/**
 * Human-readable messages for [Slf4jKtxPluginErrors]. Registered through the
 * [DefaultErrorMessages.Extension] mechanism.
 */
object Slf4jKtxPluginErrorsRendering : DefaultErrorMessages.Extension {

    private val MAP = DiagnosticFactoryToRendererMap("slf4j-ktx").apply {
        put(
            Slf4jKtxPluginErrors.CORE_MISSING,
            "slf4j-ktx-core is not on the compile classpath. Add io.github.harryjhin:slf4j-ktx-core as a dependency.",
        )
        put(
            Slf4jKtxPluginErrors.CORE_TOO_OLD,
            "slf4j-ktx-core version {0} is older than the minimum supported {1}. Upgrade the slf4j-ktx-core dependency.",
            CommonRenderers.STRING,
            CommonRenderers.STRING,
        )
        put(
            Slf4jKtxPluginErrors.COMPILER_TOO_OLD,
            "Current Kotlin compiler {0} is older than required {1} declared by slf4j-ktx-core. Upgrade the Kotlin plugin.",
            CommonRenderers.STRING,
            CommonRenderers.STRING,
        )
    }

    override fun getMap(): DiagnosticFactoryToRendererMap = MAP
}
