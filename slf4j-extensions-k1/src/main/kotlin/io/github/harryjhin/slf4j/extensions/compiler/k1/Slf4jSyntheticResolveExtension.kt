package io.github.harryjhin.slf4j.extensions.compiler.k1

import org.jetbrains.kotlin.descriptors.CallableMemberDescriptor
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.descriptors.DescriptorVisibilities
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.descriptors.ModuleDescriptor
import org.jetbrains.kotlin.descriptors.PackageFragmentDescriptor
import org.jetbrains.kotlin.descriptors.PropertyDescriptor
import org.jetbrains.kotlin.descriptors.SimpleFunctionDescriptor
import org.jetbrains.kotlin.descriptors.SourceElement
import org.jetbrains.kotlin.descriptors.annotations.AnnotationDescriptorImpl
import org.jetbrains.kotlin.descriptors.annotations.Annotations
import org.jetbrains.kotlin.descriptors.findClassAcrossModuleDependencies
import org.jetbrains.kotlin.descriptors.impl.PropertyDescriptorImpl
import org.jetbrains.kotlin.descriptors.impl.PropertyGetterDescriptorImpl
import org.jetbrains.kotlin.descriptors.impl.SimpleFunctionDescriptorImpl
import org.jetbrains.kotlin.descriptors.impl.ValueParameterDescriptorImpl
import org.jetbrains.kotlin.incremental.components.NoLookupLocation
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.DescriptorUtils
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

    private companion object {
        private val JPA_ANNOTATIONS = setOf(
            "jakarta.persistence.Entity",
            "jakarta.persistence.MappedSuperclass",
            "jakarta.persistence.Embeddable",
            "javax.persistence.Entity",
            "javax.persistence.MappedSuperclass",
            "javax.persistence.Embeddable",
        )
    }

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
        if (result.isNotEmpty()) return // class already has a property with this name

        val loggerDescriptor = thisDescriptor.module
            .getPackage(FqName("org.slf4j")).memberScope
            .getContributedClassifier(
                Name.identifier("Logger"),
                NoLookupLocation.FROM_BACKEND,
            ) as? ClassDescriptor ?: return
        val loggerType = loggerDescriptor.defaultType

        val jvmSynthetic = jvmSyntheticAnnotations(thisDescriptor.module)

        val property = PropertyDescriptorImpl.create(
            thisDescriptor,
            jvmSynthetic,
            Modality.FINAL,
            DescriptorVisibilities.PRIVATE,
            false,
            name,
            CallableMemberDescriptor.Kind.SYNTHESIZED,
            thisDescriptor.source,
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

        val getter = PropertyGetterDescriptorImpl(
            property, jvmSynthetic, Modality.FINAL, DescriptorVisibilities.PRIVATE,
            false, false, false,
            CallableMemberDescriptor.Kind.SYNTHESIZED, null, thisDescriptor.source,
        )
        getter.initialize(loggerType)
        property.initialize(getter, null)

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
        if (result.isNotEmpty()) return // class already has a function with this name

        val builtIns = thisDescriptor.module.builtIns
        val throwableType = builtIns.throwable.defaultType
        val unitType = builtIns.unitType
        val stringType = builtIns.stringType
        val function0OfString = KotlinTypeFactory.simpleNotNullType(
            TypeAttributes.Empty,
            builtIns.getFunction(0),
            listOf(TypeProjectionImpl(stringType)),
        )

        val jvmSynthetic = jvmSyntheticAnnotations(thisDescriptor.module)

        // fun trace(message: () -> String)
        val simpleFunc = SimpleFunctionDescriptorImpl.create(
            thisDescriptor,
            jvmSynthetic,
            name,
            CallableMemberDescriptor.Kind.SYNTHESIZED,
            thisDescriptor.source,
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
                    false, false, false, null, thisDescriptor.source,
                ),
            ),
            unitType,
            Modality.FINAL,
            DescriptorVisibilities.PRIVATE,
        )
        simpleFunc.setReturnType(unitType)
        result.add(simpleFunc)

        // fun trace(throwable: Throwable, message: () -> String)
        val throwableFunc = SimpleFunctionDescriptorImpl.create(
            thisDescriptor,
            jvmSynthetic,
            name,
            CallableMemberDescriptor.Kind.SYNTHESIZED,
            thisDescriptor.source,
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
                    false, false, false, null, thisDescriptor.source,
                ),
                ValueParameterDescriptorImpl(
                    throwableFunc, null, 1, Annotations.EMPTY,
                    Name.identifier("message"), function0OfString,
                    false, false, false, null, thisDescriptor.source,
                ),
            ),
            unitType,
            Modality.FINAL,
            DescriptorVisibilities.PRIVATE,
        )
        throwableFunc.setReturnType(unitType)
        result.add(throwableFunc)
    }

    /**
     * Builds an [Annotations] instance containing a single `@kotlin.jvm.JvmSynthetic`
     * annotation. Synthetic members carry this so kapt-generated Java stubs omit
     * them entirely, which avoids downstream annotation processors (QueryDSL APT,
     * etc.) picking up the injected `log` property as a queryable field.
     */
    private fun jvmSyntheticAnnotations(module: ModuleDescriptor): Annotations {
        val classId = ClassId(FqName("kotlin.jvm"), Name.identifier("JvmSynthetic"))
        val jvmSyntheticClass = module.findClassAcrossModuleDependencies(classId)
            ?: return Annotations.EMPTY
        return Annotations.create(
            listOf(
                AnnotationDescriptorImpl(
                    jvmSyntheticClass.defaultType,
                    emptyMap(),
                    SourceElement.NO_SOURCE,
                ),
            ),
        )
    }

    private fun shouldGenerateFor(descriptor: ClassDescriptor): Boolean {
        if (descriptor.kind == ClassKind.INTERFACE ||
            descriptor.kind == ClassKind.ANNOTATION_CLASS
        ) return false

        // Skip local and anonymous classes (declared inside functions/property initializers).
        // Injecting synthetic members into such classes corrupts IR linking of the
        // enclosing generic method's type parameters — psi2ir leaves the outer T
        // unbound after resolving our synthetic declarations in the inner scope.
        // Seen with `object : TypeReference<...>() {}` inside `fun <T> ...`.
        if (DescriptorUtils.isLocal(descriptor)) return false

        // Skip JPA entity classes. Kotlin-aware annotation processors
        // (QueryDSL APT, etc.) read the `@Metadata` annotation — not just the
        // Java stub — so `@JvmSynthetic` alone does not hide our injected
        // `log: Logger` property from them. The processors then emit invalid
        // paths in generated Q-classes (e.g. `SimplePath<Logger> log =
        // _super.log` where the supertype's Q-class has no `log`). Entities
        // are data containers that should not log anyway.
        if (descriptor.annotations.any { it.fqName?.asString() in JPA_ANNOTATIONS }) return false

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
