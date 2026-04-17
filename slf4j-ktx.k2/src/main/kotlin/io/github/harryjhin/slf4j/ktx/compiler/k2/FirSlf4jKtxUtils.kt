package io.github.harryjhin.slf4j.ktx.compiler.k2

import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.resolve.defaultType
import org.jetbrains.kotlin.fir.resolve.providers.symbolProvider
import org.jetbrains.kotlin.fir.symbols.impl.FirRegularClassSymbol
import org.jetbrains.kotlin.fir.types.ConeKotlinType
import org.jetbrains.kotlin.name.ClassId

/**
 * FIR utilities used by the K2 resolve extension. Trimmed to the single helper needed after
 * level-function synthesis moved to runtime extensions + IR rewriting (the string/throwable/
 * function0 class IDs and the `createFunction0Type` builder are no longer referenced).
 */
internal object FirSlf4jKtxUtils {

    /** Resolves [classId] to a ConeKotlinType via the session's symbol provider. */
    fun resolveType(session: FirSession, classId: ClassId): ConeKotlinType? =
        (session.symbolProvider.getClassLikeSymbolByClassId(classId) as? FirRegularClassSymbol)?.defaultType()
}
