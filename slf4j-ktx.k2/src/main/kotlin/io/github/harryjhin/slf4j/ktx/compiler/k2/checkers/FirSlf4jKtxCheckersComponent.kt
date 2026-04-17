package io.github.harryjhin.slf4j.ktx.compiler.k2.checkers

import io.github.harryjhin.slf4j.ktx.compiler.Slf4jKtxConfig
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.analysis.checkers.declaration.DeclarationCheckers
import org.jetbrains.kotlin.fir.analysis.checkers.declaration.FirClassChecker
import org.jetbrains.kotlin.fir.analysis.extensions.FirAdditionalCheckersExtension

/**
 * Registers [FirSlf4jKtxPluginClassChecker] via K2's FirAdditionalCheckersExtension. Config is
 * passed in so the checker applies the exact trigger filter the synthesis extension uses.
 */
class FirSlf4jKtxCheckersComponent(
    session: FirSession,
    config: Slf4jKtxConfig,
) : FirAdditionalCheckersExtension(session) {

    private val classChecker = FirSlf4jKtxPluginClassChecker(config)

    override val declarationCheckers: DeclarationCheckers = object : DeclarationCheckers() {
        override val classCheckers: Set<FirClassChecker> = setOf(classChecker)
    }
}
