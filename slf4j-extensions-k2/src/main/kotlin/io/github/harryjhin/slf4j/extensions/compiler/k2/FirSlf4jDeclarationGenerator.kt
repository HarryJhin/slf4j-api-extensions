package io.github.harryjhin.slf4j.extensions.compiler.k2

import io.github.harryjhin.slf4j.extensions.compiler.Slf4jExtensionsPluginKey
import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.descriptors.Visibilities
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.extensions.FirDeclarationGenerationExtension
import org.jetbrains.kotlin.fir.extensions.MemberGenerationContext
import org.jetbrains.kotlin.fir.plugin.createMemberFunction
import org.jetbrains.kotlin.fir.plugin.createMemberProperty
import org.jetbrains.kotlin.fir.resolve.defaultType
import org.jetbrains.kotlin.fir.resolve.providers.symbolProvider
import org.jetbrains.kotlin.fir.symbols.impl.FirClassSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirNamedFunctionSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirPropertySymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirRegularClassSymbol
import org.jetbrains.kotlin.fir.symbols.impl.ConeClassLikeLookupTagImpl
import org.jetbrains.kotlin.fir.types.ConeAttributes
import org.jetbrains.kotlin.fir.types.ConeKotlinType
import org.jetbrains.kotlin.fir.types.impl.ConeClassLikeTypeImpl
import org.jetbrains.kotlin.name.CallableId
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name

class FirSlf4jDeclarationGenerator(
    session: FirSession,
    private val propertyName: String,
    private val annotations: List<String>,
    private val packages: List<String>,
    private val allClasses: Boolean,
) : FirDeclarationGenerationExtension(session) {

    companion object {
        private val LOGGER_CLASS_ID = ClassId(FqName("org.slf4j"), Name.identifier("Logger"))
        private val THROWABLE_CLASS_ID = ClassId(FqName("kotlin"), Name.identifier("Throwable"))
        private val STRING_CLASS_ID = ClassId(FqName("kotlin"), Name.identifier("String"))
        private val FUNCTION0_CLASS_ID = ClassId(FqName("kotlin"), Name.identifier("Function0"))

        private val LOG_LEVEL_NAMES = listOf("trace", "debug", "info", "warn", "error")
    }

    private val propertyNameId = Name.identifier(propertyName)
    private val functionNames: Set<Name> = LOG_LEVEL_NAMES.map { Name.identifier(it) }.toSet()
    private val allCallableNames: Set<Name> = functionNames + propertyNameId

    override fun getCallableNamesForClass(
        classSymbol: FirClassSymbol<*>,
        context: MemberGenerationContext,
    ): Set<Name> {
        if (!shouldGenerateFor(classSymbol)) return emptySet()

        // Skip if class already has a property with the target name
        @OptIn(org.jetbrains.kotlin.fir.declarations.DirectDeclarationsAccess::class)
        val hasExistingLog = classSymbol.declarationSymbols.any { decl ->
            decl is FirPropertySymbol && decl.name == propertyNameId
        }
        if (hasExistingLog) return emptySet()

        return allCallableNames
    }

    override fun generateProperties(
        callableId: CallableId,
        context: MemberGenerationContext?,
    ): List<FirPropertySymbol> {
        if (callableId.callableName != propertyNameId) return emptyList()
        val owner = context?.owner ?: return emptyList()

        val loggerType = resolveType(LOGGER_CLASS_ID) ?: return emptyList()

        val property = createMemberProperty(
            owner = owner,
            key = Slf4jExtensionsPluginKey,
            name = propertyNameId,
            returnType = loggerType,
            isVal = true,
            hasBackingField = true,
        ) {
            visibility = Visibilities.Private
            modality = Modality.FINAL
            withGeneratedDefaultInitializer()
        }

        return listOf(property.symbol)
    }

    override fun generateFunctions(
        callableId: CallableId,
        context: MemberGenerationContext?,
    ): List<FirNamedFunctionSymbol> {
        if (callableId.callableName !in functionNames) return emptyList()
        val owner = context?.owner ?: return emptyList()

        val unitType = session.builtinTypes.unitType.coneType
        val stringType = resolveType(STRING_CLASS_ID) ?: return emptyList()
        val throwableType = resolveType(THROWABLE_CLASS_ID) ?: return emptyList()
        val function0OfString = createFunction0Type(stringType) ?: return emptyList()

        val results = mutableListOf<FirNamedFunctionSymbol>()

        // fun trace(message: () -> String)
        val simpleFunc = createMemberFunction(
            owner = owner,
            key = Slf4jExtensionsPluginKey,
            name = callableId.callableName,
            returnType = unitType,
        ) {
            visibility = Visibilities.Private
            modality = Modality.FINAL
            status { isInline = true }
            valueParameter(Name.identifier("message"), function0OfString)
        }
        results += simpleFunc.symbol

        // fun trace(throwable: Throwable, message: () -> String)
        val throwableFunc = createMemberFunction(
            owner = owner,
            key = Slf4jExtensionsPluginKey,
            name = callableId.callableName,
            returnType = unitType,
        ) {
            visibility = Visibilities.Private
            modality = Modality.FINAL
            status { isInline = true }
            valueParameter(Name.identifier("throwable"), throwableType)
            valueParameter(Name.identifier("message"), function0OfString)
        }
        results += throwableFunc.symbol

        return results
    }

    private fun shouldGenerateFor(classSymbol: FirClassSymbol<*>): Boolean {
        if (classSymbol !is FirRegularClassSymbol) return false

        val classKind = classSymbol.classKind
        if (classKind == ClassKind.INTERFACE || classKind == ClassKind.ANNOTATION_CLASS) return false

        if (allClasses) return true

        if (annotations.isNotEmpty()) {
            val classAnnotationFqNames = classSymbol.resolvedAnnotationClassIds.map { it.asSingleFqName().asString() }
            if (annotations.any { it in classAnnotationFqNames }) return true
        }

        if (packages.isNotEmpty()) {
            val classPkg = classSymbol.classId.packageFqName.asString()
            if (packages.any { classPkg.startsWith(it) }) return true
        }

        return false
    }

    private fun resolveType(classId: ClassId): ConeKotlinType? {
        return session.symbolProvider.getClassLikeSymbolByClassId(classId)
            ?.let { (it as? FirRegularClassSymbol)?.defaultType() }
    }

    private fun createFunction0Type(returnType: ConeKotlinType): ConeKotlinType {
        return ConeClassLikeTypeImpl(
            ConeClassLikeLookupTagImpl(FUNCTION0_CLASS_ID),
            arrayOf(returnType),
            isMarkedNullable = false,
        )
    }
}
