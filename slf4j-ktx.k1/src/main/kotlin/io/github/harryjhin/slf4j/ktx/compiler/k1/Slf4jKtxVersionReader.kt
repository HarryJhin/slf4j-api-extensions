package io.github.harryjhin.slf4j.ktx.compiler.k1

import io.github.harryjhin.slf4j.ktx.compiler.Slf4jKtxPluginNames
import io.github.harryjhin.slf4j.ktx.compiler.Slf4jKtxVersions
import org.jetbrains.kotlin.descriptors.ModuleDescriptor
import org.jetbrains.kotlin.descriptors.findClassAcrossModuleDependencies
import org.jetbrains.kotlin.load.kotlin.KotlinJvmBinarySourceElement
import org.jetbrains.kotlin.name.ClassId
import java.io.File

/**
 * K1 entry point for reading `slf4j-ktx-core`'s compatibility manifest from a [ModuleDescriptor].
 * Platform-neutral primitives ([Slf4jKtxVersions.Version], [Slf4jKtxVersions.RuntimeVersions],
 * [Slf4jKtxVersions.MINIMAL_SUPPORTED_VERSION], [Slf4jKtxVersions.readFromJar]) live in
 * `:slf4j-ktx.common` so K2 can reuse them.
 */
internal object Slf4jKtxVersionReader {

    private const val CLASS_SUFFIX = "!/io/github/harryjhin/slf4j/ktx/Slf4j.class"

    // Per-module cache. Compiler sessions are short-lived; a synchronized map is sufficient.
    private val CACHE = java.util.Collections.synchronizedMap(HashMap<ModuleDescriptor, Slf4jKtxVersions.RuntimeVersions?>())

    /** Returns null when `slf4j-ktx-core` is not present on the module's classpath. */
    fun readRuntimeVersions(module: ModuleDescriptor): Slf4jKtxVersions.RuntimeVersions? {
        if (CACHE.containsKey(module)) return CACHE[module]
        val computed = compute(module)
        CACHE[module] = computed
        return computed
    }

    private fun compute(module: ModuleDescriptor): Slf4jKtxVersions.RuntimeVersions? {
        val markerClass = module.findClassAcrossModuleDependencies(
            ClassId.topLevel(Slf4jKtxPluginNames.SLF4J_ANNOTATION_FQ_NAME)
        ) ?: return null
        val source = markerClass.source as? KotlinJvmBinarySourceElement ?: return null
        val location = source.binaryClass.location
        val jarPath = location.removeSuffix(CLASS_SUFFIX)
        if (!jarPath.endsWith(".jar")) return null
        val jarFile = File(jarPath)
        if (!jarFile.canRead()) return null
        return Slf4jKtxVersions.readFromJar(jarFile)
    }
}
