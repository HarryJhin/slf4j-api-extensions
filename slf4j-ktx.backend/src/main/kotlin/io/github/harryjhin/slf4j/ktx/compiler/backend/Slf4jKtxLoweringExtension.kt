package io.github.harryjhin.slf4j.ktx.compiler.backend

import org.jetbrains.kotlin.backend.common.extensions.IrGenerationExtension
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment
import org.jetbrains.kotlin.ir.visitors.transformChildrenVoid

/**
 * Backend entry point — wires the shared [Slf4jKtxPluginContext] and dispatches
 * [Slf4jKtxIrGenerator] as a transformer across the whole module. The generator both
 * fills plugin-synthesized `log` property bodies and rewrites runtime-extension call sites
 * (see [Slf4jKtxIrGenerator] docs).
 */
class Slf4jKtxLoweringExtension : IrGenerationExtension {

    override fun generate(moduleFragment: IrModuleFragment, pluginContext: IrPluginContext) {
        val ctx = Slf4jKtxPluginContext(pluginContext)
        val generator = Slf4jKtxIrGenerator(ctx)
        moduleFragment.transformChildrenVoid(generator)
    }
}
