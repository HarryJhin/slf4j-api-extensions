package io.github.harryjhin.slf4j.ktx.compiler.k1

import io.github.harryjhin.slf4j.ktx.compiler.Slf4jKtxConfig
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.descriptors.findClassAcrossModuleDependencies
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.resolve.DescriptorUtils
import org.jetbrains.kotlin.resolve.descriptorUtil.module

/**
 * Shared trigger predicate for the K1 frontend. Decides whether the plugin should treat a
 * given class/object as a generation + call-rewrite site — i.e. whether to synthesize the
 * Companion's `log` property on it (via [Slf4jKtxResolveExtension]) and whether the IR pass
 * should redirect runtime-extension calls made against it to that property.
 *
 * Rules:
 *  - interfaces and annotation classes: skip (no useful logger surface)
 *  - local / anonymous classes: skip (generic enclosing IR linkage is fragile)
 *  - otherwise: class must carry a trigger annotation directly OR one of its annotations must
 *    itself be annotated with a trigger (flat 1-hop meta-annotation, matching the semantics
 *    of `FirSerializationPredicates.metaAnnotated(..., includeItself = false)`).
 *
 * Must stay semantically aligned with `FirSlf4jKtxClassFilter` on the K2 side so the two
 * frontends agree on which classes are triggered.
 */
internal object Slf4jKtxClassFilter {

    fun shouldGenerateFor(descriptor: ClassDescriptor, config: Slf4jKtxConfig): Boolean {
        if (descriptor.kind == ClassKind.INTERFACE || descriptor.kind == ClassKind.ANNOTATION_CLASS) return false
        if (DescriptorUtils.isLocal(descriptor)) return false
        return matchesTriggerOrMeta(descriptor, config.annotations.toSet())
    }

    private fun matchesTriggerOrMeta(descriptor: ClassDescriptor, triggers: Set<FqName>): Boolean {
        val directFqs = descriptor.annotations.mapNotNull { it.fqName }
        if (directFqs.any { it in triggers }) return true
        for (directFq in directFqs) {
            val annoClass = descriptor.module.findClassAcrossModuleDependencies(ClassId.topLevel(directFq))
                ?: continue
            if (annoClass.annotations.any { it.fqName in triggers }) return true
        }
        return false
    }
}
