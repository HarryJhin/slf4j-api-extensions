package io.github.harryjhin.slf4j.ktx.compiler.k1

import io.github.harryjhin.slf4j.ktx.compiler.Slf4jKtxConfig
import io.github.harryjhin.slf4j.ktx.compiler.Slf4jKtxEntityNames
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.descriptors.PropertyDescriptor
import org.jetbrains.kotlin.descriptors.SimpleFunctionDescriptor
import org.jetbrains.kotlin.descriptors.findClassAcrossModuleDependencies
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.name.SpecialNames
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.descriptorUtil.module
import org.jetbrains.kotlin.resolve.extensions.SyntheticResolveExtension

/**
 * K1 frontend entry point. Companion-centric synthesis:
 *  - triggered class lacking a Companion → announce one via [getSyntheticCompanionObjectNameIfNeeded]
 *  - the Companion (or the triggered standalone object) hosts `log` + 5 level × 2 overload functions
 *
 * Collision policy: if the user has already declared a member with the same name on the generation
 * site, the plugin silently skips that name (`result.isNotEmpty() → return`). Matches
 * kotlinx-serialization's SerializationResolveExtension behavior.
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

    override fun getSyntheticFunctionNames(thisDescriptor: ClassDescriptor): List<Name> =
        if (isGenerationSite(thisDescriptor)) Slf4jKtxEntityNames.LOG_LEVEL_NAMES
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

    override fun generateSyntheticMethods(
        thisDescriptor: ClassDescriptor,
        name: Name,
        bindingContext: BindingContext,
        fromSupertypes: List<SimpleFunctionDescriptor>,
        result: MutableCollection<SimpleFunctionDescriptor>,
    ) {
        if (name !in Slf4jKtxEntityNames.LOG_LEVEL_NAME_SET) return
        if (!isGenerationSite(thisDescriptor)) return
        if (result.isNotEmpty()) return                                     // user-declared — skip
        // Mirror the Logger-presence guard in createLogProperty: do not emit level functions whose
        // bodies would dereference a missing `log`.
        if (thisDescriptor.module.findClassAcrossModuleDependencies(Slf4jKtxEntityNames.LOGGER_CLASS_ID) == null) return
        result += Slf4jKtxDescriptorResolver.createLevelFunction(thisDescriptor, name, throwable = false)
        result += Slf4jKtxDescriptorResolver.createLevelFunction(thisDescriptor, name, throwable = true)
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
