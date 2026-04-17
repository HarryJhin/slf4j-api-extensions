package io.github.harryjhin.slf4j.ktx.compiler.k1

import io.github.harryjhin.slf4j.ktx.compiler.Slf4jKtxEntityNames
import org.jetbrains.kotlin.descriptors.CallableMemberDescriptor
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.DescriptorVisibilities
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.descriptors.PropertyDescriptor
import org.jetbrains.kotlin.descriptors.SimpleFunctionDescriptor
import org.jetbrains.kotlin.descriptors.annotations.Annotations
import org.jetbrains.kotlin.descriptors.findClassAcrossModuleDependencies
import org.jetbrains.kotlin.descriptors.impl.PropertyDescriptorImpl
import org.jetbrains.kotlin.descriptors.impl.PropertyGetterDescriptorImpl
import org.jetbrains.kotlin.descriptors.impl.SimpleFunctionDescriptorImpl
import org.jetbrains.kotlin.descriptors.impl.ValueParameterDescriptorImpl
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.resolve.descriptorUtil.builtIns
import org.jetbrains.kotlin.resolve.descriptorUtil.module
import org.jetbrains.kotlin.types.KotlinTypeFactory
import org.jetbrains.kotlin.types.TypeAttributes
import org.jetbrains.kotlin.types.TypeProjectionImpl

/**
 * Builds K1 descriptors for the Companion-level `log` property and the level functions.
 *
 * Visibility is [DescriptorVisibilities.INTERNAL] — Lombok-like semantics: the API is intra-module
 * only. Callers use the synthesized `info { }` / `error(e) { }` from inside the enclosing class
 * body; the `log` handle is not part of the public surface.
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

    /**
     * Creates one overload of `<level>(message: () -> String)` or
     * `<level>(throwable: Throwable, message: () -> String)`. Both return `Unit`.
     *
     * @param owner the Companion object or a triggered standalone object (the descriptor that
     *   will host the function)
     * @param name one of trace/debug/info/warn/error
     * @param throwable true for the Throwable-aware overload
     */
    fun createLevelFunction(owner: ClassDescriptor, name: Name, throwable: Boolean): SimpleFunctionDescriptor {
        val builtIns = owner.builtIns
        val unitType = builtIns.unitType
        val stringType = builtIns.stringType
        val throwableType = builtIns.throwable.defaultType
        val function0OfString = KotlinTypeFactory.simpleNotNullType(
            TypeAttributes.Empty,
            builtIns.getFunction(0),
            listOf(TypeProjectionImpl(stringType)),
        )

        val fn = SimpleFunctionDescriptorImpl.create(
            owner,
            Annotations.EMPTY,
            name,
            CallableMemberDescriptor.Kind.SYNTHESIZED,
            owner.source,
        )
        val params = buildList {
            var index = 0
            if (throwable) {
                add(
                    ValueParameterDescriptorImpl(
                        fn, null, index++, Annotations.EMPTY,
                        Name.identifier("throwable"), throwableType,
                        false, false, false, null, owner.source,
                    )
                )
            }
            add(
                ValueParameterDescriptorImpl(
                    fn, null, index, Annotations.EMPTY,
                    Name.identifier("message"), function0OfString,
                    false, false, false, null, owner.source,
                )
            )
        }
        fn.initialize(
            null,                              // extensionReceiverParameter
            owner.thisAsReceiverParameter,     // dispatchReceiverParameter
            emptyList(),                       // contextReceiverParameters
            emptyList(),                       // typeParameters
            params,                            // valueParameters
            unitType,                          // returnType
            Modality.FINAL,
            DescriptorVisibilities.INTERNAL,
        )
        fn.setReturnType(unitType)
        return fn
    }
}
