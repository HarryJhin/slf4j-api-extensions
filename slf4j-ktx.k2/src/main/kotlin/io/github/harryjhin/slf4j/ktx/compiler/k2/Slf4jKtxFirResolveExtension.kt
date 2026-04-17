package io.github.harryjhin.slf4j.ktx.compiler.k2

import io.github.harryjhin.slf4j.ktx.compiler.Slf4jKtxConfig
import io.github.harryjhin.slf4j.ktx.compiler.Slf4jKtxEntityNames
import io.github.harryjhin.slf4j.ktx.compiler.Slf4jKtxPluginKey
import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.descriptors.Visibilities
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.analysis.checkers.getContainingClassSymbol
import org.jetbrains.kotlin.fir.declarations.FirDeclarationOrigin
import org.jetbrains.kotlin.fir.declarations.utils.isCompanion
import org.jetbrains.kotlin.fir.extensions.FirDeclarationGenerationExtension
import org.jetbrains.kotlin.fir.extensions.FirDeclarationPredicateRegistrar
import org.jetbrains.kotlin.fir.extensions.MemberGenerationContext
import org.jetbrains.kotlin.fir.extensions.NestedClassGenerationContext
import org.jetbrains.kotlin.fir.plugin.createCompanionObject
import org.jetbrains.kotlin.fir.plugin.createDefaultPrivateConstructor
import org.jetbrains.kotlin.fir.plugin.createMemberFunction
import org.jetbrains.kotlin.fir.plugin.createMemberProperty
import org.jetbrains.kotlin.fir.symbols.impl.FirClassLikeSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirClassSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirConstructorSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirNamedFunctionSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirPropertySymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirRegularClassSymbol
import org.jetbrains.kotlin.name.CallableId
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.name.SpecialNames

/**
 * K2 FIR entry point — Companion-centric synthesis.
 *
 *  - Triggered class lacking a Companion: announce one via [getNestedClassifiersNames] and
 *    emit it in [generateNestedClassLikeDeclaration].
 *  - Companion of a triggered class OR triggered standalone `object`: host `log` + 5 level × 2
 *    overload functions.
 *
 * Collision policy (silent skip, matching SerializationResolveExtension):
 *  - Names already declared on the generation site are excluded in [getCallableNamesForClass].
 *  - Belt-and-braces: [generateProperties]/[generateFunctions] also check existing declarations
 *    in case the name was contributed after [getCallableNamesForClass] was consulted.
 */
class Slf4jKtxFirResolveExtension(
    session: FirSession,
    private val config: Slf4jKtxConfig,
) : FirDeclarationGenerationExtension(session) {

    override fun FirDeclarationPredicateRegistrar.registerPredicates() {
        register(FirSlf4jKtxPredicates.hasAnyTriggerAnnotation(config))
    }

    override fun getNestedClassifiersNames(
        classSymbol: FirClassSymbol<*>,
        context: NestedClassGenerationContext,
    ): Set<Name> {
        if (classSymbol !is FirRegularClassSymbol) return emptySet()
        if (!FirSlf4jKtxClassFilter.shouldGenerateFor(classSymbol, session, config)) return emptySet()
        if (classSymbol.classKind == ClassKind.OBJECT) return emptySet()
        if (classSymbol.companionObjectSymbol != null) return emptySet()
        return setOf(SpecialNames.DEFAULT_NAME_FOR_COMPANION_OBJECT)
    }

    override fun generateNestedClassLikeDeclaration(
        owner: FirClassSymbol<*>,
        name: Name,
        context: NestedClassGenerationContext,
    ): FirClassLikeSymbol<*>? {
        if (name != SpecialNames.DEFAULT_NAME_FOR_COMPANION_OBJECT) return null
        val ownerRegular = owner as? FirRegularClassSymbol ?: return null
        return createCompanionObject(ownerRegular, Slf4jKtxPluginKey).symbol
    }

    override fun getCallableNamesForClass(
        classSymbol: FirClassSymbol<*>,
        context: MemberGenerationContext,
    ): Set<Name> {
        if (!isGenerationSite(classSymbol)) return emptySet()
        val existing = classSymbol.declarationSymbols.mapNotNull { decl ->
            when (decl) {
                is FirPropertySymbol -> decl.name
                is FirNamedFunctionSymbol -> decl.name
                else -> null
            }
        }.toSet()
        val names = (Slf4jKtxEntityNames.ALL_CALLABLE_NAMES - existing).toMutableSet()
        // When THIS plugin synthesized the Companion itself, we must also emit its primary
        // constructor (see [generateConstructors]). Without this, JVM ObjectClassLowering
        // aborts with "Object should have a primary constructor: Companion".
        // Mirrors SerializationFirResolveExtension.getCallableNamesForClass (line 97-100).
        val origin = classSymbol.origin as? FirDeclarationOrigin.Plugin
        if (origin?.key == Slf4jKtxPluginKey) {
            names += SpecialNames.INIT
        }
        return names
    }

    /**
     * Emit the primary constructor for plugin-synthesized Companions.
     * Only when the owner's origin is our plugin key — we never touch user-declared Companions
     * or objects whose constructors already exist. Mirrors
     * SerializationFirResolveExtension.generateConstructors (line 296-300).
     */
    override fun generateConstructors(context: MemberGenerationContext): List<FirConstructorSymbol> {
        val owner = context.owner
        val origin = owner.origin as? FirDeclarationOrigin.Plugin ?: return emptyList()
        if (origin.key != Slf4jKtxPluginKey) return emptyList()
        return listOf(createDefaultPrivateConstructor(owner, Slf4jKtxPluginKey).symbol)
    }

    override fun generateProperties(
        callableId: CallableId,
        context: MemberGenerationContext?,
    ): List<FirPropertySymbol> {
        if (callableId.callableName != Slf4jKtxEntityNames.LOG_PROPERTY_ID) return emptyList()
        val owner = context?.owner ?: return emptyList()
        if (!isGenerationSite(owner)) return emptyList()
        if (owner.declarationSymbols.any { it is FirPropertySymbol && it.name == callableId.callableName }) {
            return emptyList()
        }
        val loggerType = FirSlf4jKtxUtils.resolveType(session, Slf4jKtxEntityNames.LOGGER_CLASS_ID)
            ?: return emptyList()

        val property = createMemberProperty(
            owner = owner,
            key = Slf4jKtxPluginKey,
            name = callableId.callableName,
            returnType = loggerType,
            isVal = true,
            hasBackingField = true,
        ) {
            visibility = Visibilities.Internal
            modality = Modality.FINAL
        }
        return listOf(property.symbol)
    }

    override fun generateFunctions(
        callableId: CallableId,
        context: MemberGenerationContext?,
    ): List<FirNamedFunctionSymbol> {
        if (callableId.callableName !in Slf4jKtxEntityNames.LOG_LEVEL_NAME_SET) return emptyList()
        val owner = context?.owner ?: return emptyList()
        if (!isGenerationSite(owner)) return emptyList()
        if (owner.declarationSymbols.any { it is FirNamedFunctionSymbol && it.name == callableId.callableName }) {
            return emptyList()
        }

        val unitType = session.builtinTypes.unitType.type
        val stringType = FirSlf4jKtxUtils.resolveType(session, FirSlf4jKtxUtils.STRING_CLASS_ID) ?: return emptyList()
        val throwableType = FirSlf4jKtxUtils.resolveType(session, FirSlf4jKtxUtils.THROWABLE_CLASS_ID) ?: return emptyList()
        val function0OfString = FirSlf4jKtxUtils.createFunction0Type(stringType)

        return listOf(
            createMemberFunction(
                owner = owner,
                key = Slf4jKtxPluginKey,
                name = callableId.callableName,
                returnType = unitType,
            ) {
                visibility = Visibilities.Internal
                modality = Modality.FINAL
                valueParameter(Name.identifier("message"), function0OfString)
            }.symbol,
            createMemberFunction(
                owner = owner,
                key = Slf4jKtxPluginKey,
                name = callableId.callableName,
                returnType = unitType,
            ) {
                visibility = Visibilities.Internal
                modality = Modality.FINAL
                valueParameter(Name.identifier("throwable"), throwableType)
                valueParameter(Name.identifier("message"), function0OfString)
            }.symbol,
        )
    }

    /** Companion of a triggered class, or the triggered object itself. */
    private fun isGenerationSite(classSymbol: FirClassSymbol<*>): Boolean {
        val regular = classSymbol as? FirRegularClassSymbol ?: return false
        val siteOwner: FirRegularClassSymbol = when {
            regular.isCompanion ->
                regular.getContainingClassSymbol(session) as? FirRegularClassSymbol ?: return false
            regular.classKind == ClassKind.OBJECT -> regular
            else -> return false
        }
        return FirSlf4jKtxClassFilter.shouldGenerateFor(siteOwner, session, config)
    }
}
