package io.github.harryjhin.slf4j.ktx.compiler.k1

import io.github.harryjhin.slf4j.ktx.compiler.Slf4jKtxEntityNames
import org.jetbrains.kotlin.descriptors.CallableMemberDescriptor
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.DescriptorVisibilities
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.descriptors.PropertyDescriptor
import org.jetbrains.kotlin.descriptors.annotations.Annotations
import org.jetbrains.kotlin.descriptors.findClassAcrossModuleDependencies
import org.jetbrains.kotlin.descriptors.impl.PropertyDescriptorImpl
import org.jetbrains.kotlin.descriptors.impl.PropertyGetterDescriptorImpl
import org.jetbrains.kotlin.resolve.descriptorUtil.module

/**
 * Builds the K1 descriptor for the Companion/object-level `log: Logger` property.
 *
 * Level functions are no longer synthesized as Companion members — they live as runtime
 * inline extensions in `slf4j-ktx-core`, and the backend IR pass rewrites call sites to hit
 * the `log` backing field directly (see `Slf4jKtxIrGenerator`).
 *
 * Visibility is [DescriptorVisibilities.INTERNAL] — Lombok-like semantics: the handle is
 * intra-module only. The primary API surface is the `info { }` / `error(e) { }` extension
 * shape exposed by the runtime library.
 */
internal object Slf4jKtxDescriptorResolver {

    /**
     * Creates the Companion/object-level `log: Logger` property. Returns null when SLF4J's
     * Logger is not on the classpath — caller short-circuits; the DeclarationChecker will later
     * surface a [Slf4jKtxPluginErrors.CORE_MISSING] diagnostic instead of crashing the compiler.
     */
    fun createLogProperty(owner: ClassDescriptor): PropertyDescriptor? {
        val loggerClass = owner.module.findClassAcrossModuleDependencies(Slf4jKtxEntityNames.LOGGER_CLASS_ID)
            ?: return null
        val loggerType = loggerClass.defaultType

        val property = PropertyDescriptorImpl.create(
            owner,
            Annotations.EMPTY,
            Modality.FINAL,
            DescriptorVisibilities.INTERNAL,
            false,
            Slf4jKtxEntityNames.LOG_PROPERTY_ID,
            CallableMemberDescriptor.Kind.SYNTHESIZED,
            owner.source,
            false, false, false, false, false, false,
        )
        property.setType(loggerType, emptyList(), owner.thisAsReceiverParameter, null)

        val getter = PropertyGetterDescriptorImpl(
            property,
            Annotations.EMPTY,
            Modality.FINAL,
            DescriptorVisibilities.INTERNAL,
            false, false, false,
            CallableMemberDescriptor.Kind.SYNTHESIZED,
            null,
            owner.source,
        )
        // K1 quirk: PropertyGetterDescriptorImpl must be initialize(returnType)'d explicitly —
        // DescriptorFactory.createDefaultGetter does not populate the return type.
        getter.initialize(loggerType)
        property.initialize(getter, null)
        return property
    }
}
