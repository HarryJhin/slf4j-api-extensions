package io.github.harryjhin.slf4j.ktx.compiler.backend

import org.jetbrains.kotlin.backend.common.extensions.IrGenerationExtension
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment

/**
 * Backend entry point — wires the shared [Slf4jKtxPluginContext] and dispatches
 * [Slf4jKtxIrGenerator] across the module's files. No per-module configuration is needed:
 * the generator identifies our declarations purely through origin (see [isFromPlugin]).
 */
class Slf4jKtxLoweringExtension : IrGenerationExtension {

    override fun generate(moduleFragment: IrModuleFragment, pluginContext: IrPluginContext) {
        val ctx = Slf4jKtxPluginContext(pluginContext)
        val generator = Slf4jKtxIrGenerator(ctx)
        for (file in moduleFragment.files) {
            file.accept(generator, null)
        }
    }
}
