package io.github.harryjhin.slf4j.ktx.compiler.k2

import io.github.harryjhin.slf4j.ktx.compiler.Slf4jKtxConfig
import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.extensions.predicateBasedProvider
import org.jetbrains.kotlin.fir.symbols.impl.FirClassSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirRegularClassSymbol

/**
 * K2 counterpart to Slf4jKtxClassFilter. Uses [FirSession.predicateBasedProvider] to match the
 * registered trigger + meta-trigger predicates built from [Slf4jKtxConfig].
 */
internal object FirSlf4jKtxClassFilter {

    fun shouldGenerateFor(
        classSymbol: FirClassSymbol<*>,
        session: FirSession,
        config: Slf4jKtxConfig,
    ): Boolean {
        if (classSymbol !is FirRegularClassSymbol) return false
        val kind = classSymbol.classKind
        if (kind == ClassKind.INTERFACE || kind == ClassKind.ANNOTATION_CLASS) return false
        if (classSymbol.classId.isLocal) return false
        return session.predicateBasedProvider.matches(
            FirSlf4jKtxPredicates.hasAnyTriggerAnnotation(config),
            classSymbol,
        )
    }
}
