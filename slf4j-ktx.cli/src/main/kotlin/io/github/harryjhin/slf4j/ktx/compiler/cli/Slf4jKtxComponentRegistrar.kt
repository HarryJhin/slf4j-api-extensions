package io.github.harryjhin.slf4j.ktx.compiler.cli

import io.github.harryjhin.slf4j.ktx.compiler.Slf4jKtxConfig
import io.github.harryjhin.slf4j.ktx.compiler.backend.Slf4jKtxLoweringExtension
import io.github.harryjhin.slf4j.ktx.compiler.k1.Slf4jKtxDescriptorSerializerPlugin
import io.github.harryjhin.slf4j.ktx.compiler.k1.Slf4jKtxPluginComponentContainerContributor
import io.github.harryjhin.slf4j.ktx.compiler.k1.Slf4jKtxResolveExtension
import io.github.harryjhin.slf4j.ktx.compiler.k2.FirSlf4jKtxExtensionRegistrar
import org.jetbrains.kotlin.backend.common.extensions.IrGenerationExtension
import org.jetbrains.kotlin.compiler.plugin.CompilerPluginRegistrar
import org.jetbrains.kotlin.compiler.plugin.ExperimentalCompilerApi
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.extensions.StorageComponentContainerContributor
import org.jetbrains.kotlin.fir.extensions.FirExtensionRegistrarAdapter
import org.jetbrains.kotlin.resolve.extensions.SyntheticResolveExtension
import org.jetbrains.kotlin.serialization.DescriptorSerializerPlugin

/**
 * Compiler-plugin entry point for slf4j-ktx. Registers K1 + K2 + IR extensions unconditionally
 * (Register-All pattern — the compiler decides the active frontend based on `languageVersion`).
 *
 * `supportsK2 = true` — the registrar runs for both K1 and K2 pipelines. See
 * kotlinx-serialization's `SerializationComponentRegistrar` for the reference shape.
 */
@OptIn(ExperimentalCompilerApi::class)
class Slf4jKtxComponentRegistrar : CompilerPluginRegistrar() {

    override val supportsK2: Boolean = true

    override fun ExtensionStorage.registerExtensions(configuration: CompilerConfiguration) {
        val config = Slf4jKtxConfig.from(configuration)

        // K1 descriptor-metadata hook (empty today; reserved for phantom-Companion filtering).
        DescriptorSerializerPlugin.registerExtension(Slf4jKtxDescriptorSerializerPlugin())

        // K1 frontend — Companion auto-generation + Companion member synthesis.
        SyntheticResolveExtension.registerExtension(Slf4jKtxResolveExtension(config))

        // K1 diagnostics — runtime/compiler compatibility checks (CORE_MISSING/TOO_OLD/COMPILER_TOO_OLD).
        StorageComponentContainerContributor.registerExtension(
            Slf4jKtxPluginComponentContainerContributor(config)
        )

        // K2 FIR — declaration generation + checkers + version reader.
        FirExtensionRegistrarAdapter.registerExtension(FirSlf4jKtxExtensionRegistrar(config))

        // IR backend — fills bodies for the plugin-generated Companion/object members.
        // No per-module config needed; origin-based identification (`isFromPlugin`) is enough.
        IrGenerationExtension.registerExtension(Slf4jKtxLoweringExtension())
    }
}
