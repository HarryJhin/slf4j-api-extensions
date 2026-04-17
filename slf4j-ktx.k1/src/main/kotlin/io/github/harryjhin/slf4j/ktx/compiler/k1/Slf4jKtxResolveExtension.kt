package io.github.harryjhin.slf4j.ktx.compiler.k1

import io.github.harryjhin.slf4j.ktx.compiler.Slf4jKtxConfig
import io.github.harryjhin.slf4j.ktx.compiler.Slf4jKtxEntityNames
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.descriptors.PropertyDescriptor
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.name.SpecialNames
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.extensions.SyntheticResolveExtension

/**
 * K1 frontend entry point. Responsibilities (after the IR-rewriting refactor):
 *  - Announce a Companion on triggered classes that lack one ([getSyntheticCompanionObjectNameIfNeeded]).
 *  - Synthesize the single `log: Logger` property on the generation site (Companion of a
 *    triggered class, or the triggered standalone object itself).
 *
 * Level functions (`trace/debug/info/warn/error`) are **NOT** synthesized here anymore. They
 * live as `T.trace/.../.error × 2` top-level inline extensions in `slf4j-ktx-core`. The
 * compiler plugin's backend IR pass rewrites every call site that targets a triggered class
 * into a direct `Companion.log.<level>(...)` access, eliminating both the reflection fallback
 * and the need for IDE-side synthetic-member resolution. See `Slf4jKtxIrGenerator`.
 *
 * Collision policy: if the user has declared their own `log` member on the generation site,
 * the plugin silently skips synthesis (`result.isNotEmpty() → return`). The user's property is
 * used by the IR rewriter so long as its name matches `log`.
 */
class Slf4jKtxResolveExtension(
    private val config: Slf4jKtxConfig,
) : SyntheticResolveExtension {

    override fun getSyntheticCompanionObjectNameIfNeeded(thisDescriptor: ClassDescriptor): Name? {
        if (!Slf4jKtxClassFilter.shouldGenerateFor(thisDescriptor, config)) return null
        if (thisDescriptor.kind == ClassKind.OBJECT) return null           // objects are their own singleton
        // Do NOT query `thisDescriptor.companionObjectDescriptor` here — it triggers a recursive
        // lazy-value resolve under LockBasedStorageManager. Kotlin only invokes this hook for
        // classes that still need a companion, so the guard is unnecessary. Mirrors the shape
        // of SerializationResolveExtension.getSyntheticCompanionObjectNameIfNeeded (v1.9.25),
        // which deliberately avoids the same cycle.
        return SpecialNames.DEFAULT_NAME_FOR_COMPANION_OBJECT
    }

    override fun getSyntheticPropertiesNames(thisDescriptor: ClassDescriptor): List<Name> =
        if (isGenerationSite(thisDescriptor)) listOf(Slf4jKtxEntityNames.LOG_PROPERTY_ID)
        else emptyList()

    override fun generateSyntheticProperties(
        thisDescriptor: ClassDescriptor,
        name: Name,
        bindingContext: BindingContext,
        fromSupertypes: ArrayList<PropertyDescriptor>,
        result: MutableSet<PropertyDescriptor>,
    ) {
        if (name != Slf4jKtxEntityNames.LOG_PROPERTY_ID) return
        if (!isGenerationSite(thisDescriptor)) return
        if (result.isNotEmpty()) return                                     // user-declared — skip
        val property = Slf4jKtxDescriptorResolver.createLogProperty(thisDescriptor) ?: return
        result += property
    }

    /** Companion of a triggered class, or the triggered object itself. */
    private fun isGenerationSite(descriptor: ClassDescriptor): Boolean = when {
        descriptor.isCompanionObject -> {
            val owner = descriptor.containingDeclaration as? ClassDescriptor
            owner != null && Slf4jKtxClassFilter.shouldGenerateFor(owner, config)
        }
        descriptor.kind == ClassKind.OBJECT ->
            Slf4jKtxClassFilter.shouldGenerateFor(descriptor, config)
        else -> false
    }
}
