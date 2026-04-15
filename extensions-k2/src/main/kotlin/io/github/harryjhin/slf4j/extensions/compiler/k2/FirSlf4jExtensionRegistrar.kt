package io.github.harryjhin.slf4j.extensions.compiler.k2

import org.jetbrains.kotlin.fir.extensions.FirDeclarationGenerationExtension
import org.jetbrains.kotlin.fir.extensions.FirExtensionRegistrar

class FirSlf4jExtensionRegistrar(
    private val propertyName: String,
    private val annotations: List<String>,
    private val packages: List<String>,
    private val allClasses: Boolean,
) : FirExtensionRegistrar() {

    override fun ExtensionRegistrarContext.configurePlugin() {
        +FirDeclarationGenerationExtension.Factory { session ->
            FirSlf4jDeclarationGenerator(session, propertyName, annotations, packages, allClasses)
        }
    }
}
