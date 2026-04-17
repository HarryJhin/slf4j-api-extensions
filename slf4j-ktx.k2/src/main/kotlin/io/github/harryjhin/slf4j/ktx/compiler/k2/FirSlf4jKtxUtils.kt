package io.github.harryjhin.slf4j.ktx.compiler.k2

import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.resolve.defaultType
import org.jetbrains.kotlin.fir.resolve.providers.symbolProvider
import org.jetbrains.kotlin.fir.symbols.impl.FirRegularClassSymbol
import org.jetbrains.kotlin.fir.symbols.impl.ConeClassLikeLookupTagImpl
import org.jetbrains.kotlin.fir.types.ConeKotlinType
import org.jetbrains.kotlin.fir.types.impl.ConeClassLikeTypeImpl
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name

/**
 * FIR utilities used by the K2 resolve extension.
 */
internal object FirSlf4jKtxUtils {

    val STRING_CLASS_ID: ClassId = ClassId(FqName("kotlin"), Name.identifier("String"))
    val THROWABLE_CLASS_ID: ClassId = ClassId(FqName("kotlin"), Name.identifier("Throwable"))
    val FUNCTION0_CLASS_ID: ClassId = ClassId(FqName("kotlin"), Name.identifier("Function0"))

    /** Resolves [classId] to a ConeKotlinType via the session's symbol provider. */
    fun resolveType(session: FirSession, classId: ClassId): ConeKotlinType? =
        (session.symbolProvider.getClassLikeSymbolByClassId(classId) as? FirRegularClassSymbol)?.defaultType()

    /** Builds `Function0<returnType>` (i.e. `() -> returnType`). */
    fun createFunction0Type(returnType: ConeKotlinType): ConeKotlinType =
        ConeClassLikeTypeImpl(
            ConeClassLikeLookupTagImpl(FUNCTION0_CLASS_ID),
            arrayOf(returnType),
            isNullable = false,
        )
}
