package io.github.harryjhin.slf4j.ktx.compiler.k1

import org.jetbrains.kotlin.diagnostics.DiagnosticFactory0
import org.jetbrains.kotlin.diagnostics.DiagnosticFactory2
import org.jetbrains.kotlin.diagnostics.Errors
import org.jetbrains.kotlin.diagnostics.Severity
import org.jetbrains.kotlin.psi.KtDeclaration

/**
 * Diagnostic factories for K1 compatibility checks. Registers itself with
 * [Slf4jKtxPluginErrorsRendering] via [Errors.Initializer.initializeFactoryNamesAndDefaultErrorMessages]
 * in the init block — this pattern matches kotlinx-serialization's SerializationErrors.
 */
object Slf4jKtxPluginErrors {

    @JvmField
    val CORE_MISSING: DiagnosticFactory0<KtDeclaration> =
        DiagnosticFactory0.create(Severity.ERROR)

    /** implementationVersion, MINIMAL_SUPPORTED_VERSION */
    @JvmField
    val CORE_TOO_OLD: DiagnosticFactory2<KtDeclaration, String, String> =
        DiagnosticFactory2.create(Severity.ERROR)

    /** currentCompilerVersion, requireKotlinVersion */
    @JvmField
    val COMPILER_TOO_OLD: DiagnosticFactory2<KtDeclaration, String, String> =
        DiagnosticFactory2.create(Severity.ERROR)

    init {
        Errors.Initializer.initializeFactoryNamesAndDefaultErrorMessages(
            Slf4jKtxPluginErrors::class.java,
            Slf4jKtxPluginErrorsRendering,
        )
    }
}
