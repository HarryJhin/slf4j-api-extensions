package io.github.harryjhin.slf4j.ktx.compiler.k2.checkers

import org.jetbrains.kotlin.diagnostics.KtDiagnosticFactoryToRendererMap
import org.jetbrains.kotlin.diagnostics.rendering.BaseDiagnosticRendererFactory
import org.jetbrains.kotlin.diagnostics.rendering.CommonRenderers

/**
 * Renderer map for K2 `slf4j-ktx` diagnostics.
 */
object KtDefaultErrorMessagesSlf4jKtx : BaseDiagnosticRendererFactory() {

    override val MAP: KtDiagnosticFactoryToRendererMap =
        KtDiagnosticFactoryToRendererMap("slf4j-ktx").apply {
            put(
                FirSlf4jKtxErrors.CORE_MISSING,
                "slf4j-ktx-core is not on the compile classpath. Add io.github.harryjhin:slf4j-ktx-core as a dependency.",
            )
            put(
                FirSlf4jKtxErrors.CORE_TOO_OLD,
                "slf4j-ktx-core version {0} is older than the minimum supported {1}. Upgrade the slf4j-ktx-core dependency.",
                CommonRenderers.STRING,
                CommonRenderers.STRING,
            )
            put(
                FirSlf4jKtxErrors.COMPILER_TOO_OLD,
                "Current Kotlin compiler {0} is older than required {1} declared by slf4j-ktx-core. Upgrade the Kotlin plugin.",
                CommonRenderers.STRING,
                CommonRenderers.STRING,
            )
        }
}
