package io.github.harryjhin.slf4j.extensions.compiler.k1

import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.descriptors.PackageFragmentDescriptor
import org.jetbrains.kotlin.descriptors.PropertyDescriptor
import org.jetbrains.kotlin.descriptors.SimpleFunctionDescriptor
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.extensions.SyntheticResolveExtension

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
        // K1 descriptor generation deferred — IR full generation handles K1 path
        // TODO: Implement full K1 descriptors for IDE autocomplete support
    }

    override fun generateSyntheticMethods(
        thisDescriptor: ClassDescriptor,
        name: Name,
        bindingContext: BindingContext,
        fromSupertypes: List<SimpleFunctionDescriptor>,
        result: MutableCollection<SimpleFunctionDescriptor>,
    ) {
        // K1 descriptor generation deferred — IR full generation handles K1 path
        // TODO: Implement full K1 descriptors for IDE autocomplete support
    }

    private fun shouldGenerateFor(descriptor: ClassDescriptor): Boolean {
        if (descriptor.kind == ClassKind.INTERFACE ||
            descriptor.kind == ClassKind.ANNOTATION_CLASS) return false

        if (allClasses) return true

        if (annotations.isNotEmpty()) {
            if (descriptor.annotations.any { ann ->
                    annotations.any { it == ann.fqName?.asString() }
                }) return true
        }

        if (packages.isNotEmpty()) {
            val packageName = (descriptor.containingDeclaration as? PackageFragmentDescriptor)
                ?.fqName?.asString() ?: ""
            if (packages.any { packageName.startsWith(it) }) return true
        }

        return false
    }
}
