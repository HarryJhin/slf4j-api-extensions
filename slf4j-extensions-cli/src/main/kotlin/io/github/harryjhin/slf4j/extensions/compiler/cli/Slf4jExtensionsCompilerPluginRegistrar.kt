package io.github.harryjhin.slf4j.extensions.compiler.cli

import io.github.harryjhin.slf4j.extensions.compiler.Slf4jExtensionsConfigurationKeys
import io.github.harryjhin.slf4j.extensions.compiler.Slf4jExtensionsPluginNames
import io.github.harryjhin.slf4j.extensions.compiler.backend.Slf4jIrGenerationExtension
import io.github.harryjhin.slf4j.extensions.compiler.k1.Slf4jSyntheticResolveExtension
import io.github.harryjhin.slf4j.extensions.compiler.k2.FirSlf4jExtensionRegistrar
import org.jetbrains.kotlin.backend.common.extensions.IrGenerationExtension
import org.jetbrains.kotlin.compiler.plugin.CompilerPluginRegistrar
import org.jetbrains.kotlin.compiler.plugin.ExperimentalCompilerApi
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.fir.extensions.FirExtensionRegistrarAdapter
import org.jetbrains.kotlin.resolve.extensions.SyntheticResolveExtension

@OptIn(ExperimentalCompilerApi::class)
class Slf4jExtensionsCompilerPluginRegistrar : CompilerPluginRegistrar() {

    override val supportsK2: Boolean = true

    override fun ExtensionStorage.registerExtensions(configuration: CompilerConfiguration) {
        Companion.registerExtensions(this, configuration)
    }

    companion object {
        fun registerExtensions(
            extensionStorage: ExtensionStorage,
            configuration: CompilerConfiguration,
        ) = with(extensionStorage) {
            val propertyName = configuration.get(Slf4jExtensionsConfigurationKeys.PROPERTY_NAME)
                ?: Slf4jExtensionsPluginNames.DEFAULT_PROPERTY_NAME
            val annotations = configuration.get(Slf4jExtensionsConfigurationKeys.ANNOTATIONS)
                ?: emptyList()
            val excludeAnnotations = configuration.get(Slf4jExtensionsConfigurationKeys.EXCLUDE_ANNOTATIONS)
                ?: emptyList()
            val packages = configuration.get(Slf4jExtensionsConfigurationKeys.PACKAGES)
                ?: emptyList()
            val allClasses = configuration.get(Slf4jExtensionsConfigurationKeys.ALL_CLASSES)
                ?: true

            // K1 frontend
            SyntheticResolveExtension.registerExtension(
                Slf4jSyntheticResolveExtension(propertyName, annotations, excludeAnnotations, packages, allClasses)
            )

            // K2 frontend (FIR) — for IntelliJ K2 mode analysis and Kotlin 2.0+
            // compiler runs. Note: Kotlin 1.9.25's K2 is Beta, so this runs only
            // when the consumer opts into K2 or when the IDE (K2 mode) triggers
            // FIR analysis. The K1 SyntheticResolveExtension above handles the
            // default K1 compilation path for Kotlin 1.9.25.
            FirExtensionRegistrarAdapter.registerExtension(
                FirSlf4jExtensionRegistrar(propertyName, annotations, excludeAnnotations, packages, allClasses)
            )

            // IR backend
            IrGenerationExtension.registerExtension(
                Slf4jIrGenerationExtension(propertyName)
            )
        }
    }
}
