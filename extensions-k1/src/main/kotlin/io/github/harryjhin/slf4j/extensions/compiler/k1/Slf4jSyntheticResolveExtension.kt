package io.github.harryjhin.slf4j.extensions.compiler.k1

import org.jetbrains.kotlin.descriptors.CallableMemberDescriptor
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.descriptors.DescriptorVisibilities
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.descriptors.PackageFragmentDescriptor
import org.jetbrains.kotlin.descriptors.PropertyDescriptor
import org.jetbrains.kotlin.descriptors.SimpleFunctionDescriptor
import org.jetbrains.kotlin.descriptors.SourceElement
import org.jetbrains.kotlin.descriptors.annotations.Annotations
import org.jetbrains.kotlin.descriptors.impl.PropertyDescriptorImpl
import org.jetbrains.kotlin.descriptors.impl.SimpleFunctionDescriptorImpl
import org.jetbrains.kotlin.descriptors.impl.ValueParameterDescriptorImpl
import org.jetbrains.kotlin.incremental.components.NoLookupLocation
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.DescriptorFactory
import org.jetbrains.kotlin.resolve.descriptorUtil.builtIns
import org.jetbrains.kotlin.resolve.descriptorUtil.module
import org.jetbrains.kotlin.resolve.extensions.SyntheticResolveExtension
import org.jetbrains.kotlin.types.KotlinTypeFactory
import org.jetbrains.kotlin.types.TypeAttributes
import org.jetbrains.kotlin.types.TypeProjectionImpl

class Slf4jSyntheticResolveExtension(
    private val propertyName: String,
    private val annotations: List<String>,
    private val packages: List<String>,
    private val allClasses: Boolean,
) : SyntheticResolveExtension {

    private val propertyNameId = Name.identifier(propertyName)

    private val functionNames = listOf("trace", "debug", "info", "warn", "error")
        .map { Name.identifier(it) }

    override fun getSyntheticPropertiesNames(thisDescriptor: ClassDescriptor): List<Name> {
        if (!shouldGenerateFor(thisDescriptor)) return emptyList()
        return listOf(propertyNameId)
    }

    override fun getSyntheticFunctionNames(thisDescriptor: ClassDescriptor): List<Name> {
        if (!shouldGenerateFor(thisDescriptor)) return emptyList()
        return functionNames
    }

    override fun generateSyntheticProperties(
        thisDescriptor: ClassDescriptor,
        name: Name,
        bindingContext: BindingContext,
        fromSupertypes: ArrayList<PropertyDescriptor>,
        result: MutableSet<PropertyDescriptor>,
    ) {
        if (name != propertyNameId) return
        if (!shouldGenerateFor(thisDescriptor)) return

        val loggerDescriptor = thisDescriptor.module
            .getPackage(FqName("org.slf4j")).memberScope
            .getContributedClassifier(
                Name.identifier("Logger"),
                NoLookupLocation.FROM_BACKEND,
            ) as? ClassDescriptor ?: return
        val loggerType = loggerDescriptor.defaultType

        val property = PropertyDescriptorImpl.create(
            thisDescriptor,
            Annotations.EMPTY,
            Modality.FINAL,
            DescriptorVisibilities.PRIVATE,
            false,
            name,
            CallableMemberDescriptor.Kind.SYNTHESIZED,
            SourceElement.NO_SOURCE,
            false,
            false,
            false,
            false,
            false,
            false,
        )

        property.setType(
            loggerType,
            emptyList(),
            thisDescriptor.thisAsReceiverParameter,
            null,
        )

        property.initialize(
            DescriptorFactory.createDefaultGetter(property, Annotations.EMPTY),
            null,
        )

        result.add(property)
    }

    override fun generateSyntheticMethods(
        thisDescriptor: ClassDescriptor,
        name: Name,
        bindingContext: BindingContext,
        fromSupertypes: List<SimpleFunctionDescriptor>,
        result: MutableCollection<SimpleFunctionDescriptor>,
    ) {
        if (name !in functionNames) return
        if (!shouldGenerateFor(thisDescriptor)) return

        val builtIns = thisDescriptor.module.builtIns
        val throwableType = builtIns.throwable.defaultType
        val unitType = builtIns.unitType
        val stringType = builtIns.stringType
        val function0OfString = KotlinTypeFactory.simpleNotNullType(
            TypeAttributes.Empty,
            builtIns.getFunction(0),
            listOf(TypeProjectionImpl(stringType)),
        )

        // fun trace(message: () -> String)
        val simpleFunc = SimpleFunctionDescriptorImpl.create(
            thisDescriptor,
            Annotations.EMPTY,
            name,
            CallableMemberDescriptor.Kind.SYNTHESIZED,
            SourceElement.NO_SOURCE,
        )
        simpleFunc.initialize(
            null,
            thisDescriptor.thisAsReceiverParameter,
            emptyList(),
            emptyList(),
            listOf(
                ValueParameterDescriptorImpl(
                    simpleFunc, null, 0, Annotations.EMPTY,
                    Name.identifier("message"), function0OfString,
                    false, false, false, null, SourceElement.NO_SOURCE,
                ),
            ),
            unitType,
            Modality.FINAL,
            DescriptorVisibilities.PRIVATE,
        )
        result.add(simpleFunc)

        // fun trace(throwable: Throwable, message: () -> String)
        val throwableFunc = SimpleFunctionDescriptorImpl.create(
            thisDescriptor,
            Annotations.EMPTY,
            name,
            CallableMemberDescriptor.Kind.SYNTHESIZED,
            SourceElement.NO_SOURCE,
        )
        throwableFunc.initialize(
            null,
            thisDescriptor.thisAsReceiverParameter,
            emptyList(),
            emptyList(),
            listOf(
                ValueParameterDescriptorImpl(
                    throwableFunc, null, 0, Annotations.EMPTY,
                    Name.identifier("throwable"), throwableType,
                    false, false, false, null, SourceElement.NO_SOURCE,
                ),
                ValueParameterDescriptorImpl(
                    throwableFunc, null, 1, Annotations.EMPTY,
                    Name.identifier("message"), function0OfString,
                    false, false, false, null, SourceElement.NO_SOURCE,
                ),
            ),
            unitType,
            Modality.FINAL,
            DescriptorVisibilities.PRIVATE,
        )
        result.add(throwableFunc)
    }

    private fun shouldGenerateFor(descriptor: ClassDescriptor): Boolean {
        if (descriptor.kind == ClassKind.INTERFACE ||
            descriptor.kind == ClassKind.ANNOTATION_CLASS
        ) return false

        if (allClasses) return true

        if (annotations.isNotEmpty()) {
            if (descriptor.annotations.any { ann ->
                    annotations.any { it == ann.fqName?.asString() }
                }
            ) return true
        }

        if (packages.isNotEmpty()) {
            val packageName = (descriptor.containingDeclaration as? PackageFragmentDescriptor)
                ?.fqName?.asString() ?: ""
            if (packages.any { packageName.startsWith(it) }) return true
        }

        return false
    }
}
