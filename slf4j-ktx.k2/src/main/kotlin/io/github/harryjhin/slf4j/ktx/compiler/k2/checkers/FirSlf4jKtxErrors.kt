package io.github.harryjhin.slf4j.ktx.compiler.k2.checkers

import org.jetbrains.kotlin.diagnostics.error0
import org.jetbrains.kotlin.diagnostics.error2
import org.jetbrains.kotlin.diagnostics.rendering.RootDiagnosticRendererFactory
import org.jetbrains.kotlin.psi.KtElement

/**
 * K2 diagnostic factories for `slf4j-ktx`. Registers its renderer via
 * `RootDiagnosticRendererFactory.registerFactory(...)` in its init block — this matches the
 * FirJvmErrors / FirNativeErrors pattern. No `registerDiagnosticContainers` (Kotlin 2.x+ API
 * that does not exist in 1.9.25; see PLAN Appendix A).
 */
object FirSlf4jKtxErrors {

    val CORE_MISSING by error0<KtElement>()

    /** implementationVersion, MINIMAL_SUPPORTED_VERSION */
    val CORE_TOO_OLD by error2<KtElement, String, String>()

    /** currentCompilerVersion, requireKotlinVersion */
    val COMPILER_TOO_OLD by error2<KtElement, String, String>()

    init {
        RootDiagnosticRendererFactory.registerFactory(KtDefaultErrorMessagesSlf4jKtx)
    }
}
