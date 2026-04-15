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

    override val pluginId: String = Slf4jExtensionsPluginNames.PLUGIN_ID

    override val supportsK2: Boolean = true

    override fun ExtensionStorage.registerExtensions(configuration: CompilerConfiguration) {
        val propertyName = configuration.get(Slf4jExtensionsConfigurationKeys.PROPERTY_NAME)
            ?: Slf4jExtensionsPluginNames.DEFAULT_PROPERTY_NAME
        val annotations = configuration.get(Slf4jExtensionsConfigurationKeys.ANNOTATIONS) ?: emptyList()
        val packages = configuration.get(Slf4jExtensionsConfigurationKeys.PACKAGES) ?: emptyList()
        val allClasses = configuration.get(Slf4jExtensionsConfigurationKeys.ALL_CLASSES) ?: true

        // K1 frontend
        SyntheticResolveExtension.registerExtension(
            Slf4jSyntheticResolveExtension(propertyName, annotations, packages, allClasses)
        )

        // K2 FIR frontend
        FirExtensionRegistrarAdapter.registerExtension(
            FirSlf4jExtensionRegistrar(propertyName, annotations, packages, allClasses)
        )

        // IR backend (shared for K1 and K2)
        IrGenerationExtension.registerExtension(
            Slf4jIrGenerationExtension(propertyName)
        )
    }
}
