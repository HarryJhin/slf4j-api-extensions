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
 * Compiler-plugin entry point for slf4j-ktx. Registers all five extensions — K1 frontend,
 * K1 descriptor-metadata hook, K1 diagnostic checker, K2 FIR frontend, and the shared IR
 * backend — unconditionally (Register-All pattern). The compiler picks the active frontend
 * by `languageVersion`; `supportsK2 = true` lets the same registrar serve both pipelines.
 *
 * Extension responsibilities (summary):
 *  - `Slf4jKtxResolveExtension` (K1) / `FirSlf4jKtxExtensionRegistrar` (K2) — announce a
 *    Companion on triggered classes and synthesize the single `log: Logger` property on it.
 *    Level functions (`trace/debug/…`) are **not** synthesized anymore; they live as runtime
 *    `T.<level>(...)` inline extensions in `slf4j-ktx-core`.
 *  - `Slf4jKtxPluginComponentContainerContributor` / FIR checker component — emit
 *    CORE_MISSING / CORE_TOO_OLD / COMPILER_TOO_OLD diagnostics at trigger sites when the
 *    runtime library is absent or incompatible.
 *  - `Slf4jKtxDescriptorSerializerPlugin` — metadata hook (empty today; reserved for
 *    phantom-Companion filtering).
 *  - `Slf4jKtxLoweringExtension` — IR pass that (a) fills the plugin-synthesized `log`
 *    property's backing field + initializer + getter and (b) rewrites every call to the
 *    runtime level extensions made from a triggered class into a direct
 *    `Companion.log.<level>(...)` access. Non-triggered classes fall through to the
 *    extensions' default body.
 *
 * Reference shape: kotlinx-serialization's `SerializationComponentRegistrar`.
 */
@OptIn(ExperimentalCompilerApi::class)
class Slf4jKtxComponentRegistrar : CompilerPluginRegistrar() {

    override val supportsK2: Boolean = true

    override fun ExtensionStorage.registerExtensions(configuration: CompilerConfiguration) {
        val config = Slf4jKtxConfig.from(configuration)

        // K1 descriptor-metadata hook (no-op; kept as a registration point for future
        // phantom-Companion filtering if that becomes observable in downstream metadata).
        DescriptorSerializerPlugin.registerExtension(Slf4jKtxDescriptorSerializerPlugin())

        // K1 frontend — Companion auto-generation + `log` property synthesis.
        SyntheticResolveExtension.registerExtension(Slf4jKtxResolveExtension(config))

        // K1 diagnostics — runtime/compiler compatibility checks.
        StorageComponentContainerContributor.registerExtension(
            Slf4jKtxPluginComponentContainerContributor(config)
        )

        // K2 FIR — Companion + `log` declaration generation, checkers, version-reader session component.
        FirExtensionRegistrarAdapter.registerExtension(FirSlf4jKtxExtensionRegistrar(config))

        // IR backend — fills `log` property bodies AND rewrites runtime-extension call sites.
        // Config-free: the generator identifies targets through origin (plugin-synthesized `log`).
        IrGenerationExtension.registerExtension(Slf4jKtxLoweringExtension())
    }
}
