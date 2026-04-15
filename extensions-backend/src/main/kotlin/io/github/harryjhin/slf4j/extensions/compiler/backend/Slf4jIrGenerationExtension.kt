package io.github.harryjhin.slf4j.extensions.compiler.backend

import org.jetbrains.kotlin.backend.common.extensions.IrGenerationExtension
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment

class Slf4jIrGenerationExtension(
    private val propertyName: String,
) : IrGenerationExtension {

    override fun generate(moduleFragment: IrModuleFragment, pluginContext: IrPluginContext) {
        val transformer = Slf4jIrTransformer(pluginContext, propertyName)
        moduleFragment.files.forEach { file ->
            file.accept(transformer, null)
        }
    }
}
