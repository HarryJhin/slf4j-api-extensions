package io.github.harryjhin.slf4j.ktx.runners

import io.github.harryjhin.slf4j.ktx.compiler.cli.Slf4jKtxComponentRegistrar
import org.jetbrains.kotlin.cli.jvm.config.addJvmClasspathRoot
import org.jetbrains.kotlin.compiler.plugin.CompilerPluginRegistrar
import org.jetbrains.kotlin.compiler.plugin.ExperimentalCompilerApi
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.test.model.TestModule
import org.jetbrains.kotlin.test.services.EnvironmentConfigurator
import org.jetbrains.kotlin.test.services.TestServices

/**
 * Wires the Register-All [Slf4jKtxComponentRegistrar] into the test compiler pipeline and
 * seeds the compile classpath with the runtime JARs needed for trigger-annotation and
 * Logger-type resolution. Mirrors v1's ExtensionRegistrarConfigurator shape, adjusted for
 * the component-registrar being an instance class (not object).
 */
@OptIn(ExperimentalCompilerApi::class)
class Slf4jKtxEnvironmentConfigurator(testServices: TestServices) : EnvironmentConfigurator(testServices) {

    override fun configureCompilerConfiguration(configuration: CompilerConfiguration, module: TestModule) {
        findJarContaining("org.slf4j.Logger")?.let(configuration::addJvmClasspathRoot)
        findJarContaining("io.github.harryjhin.slf4j.ktx.Slf4j")?.let(configuration::addJvmClasspathRoot)
    }

    override fun CompilerPluginRegistrar.ExtensionStorage.registerCompilerExtensions(
        module: TestModule,
        configuration: CompilerConfiguration,
    ) {
        val registrar = Slf4jKtxComponentRegistrar()
        with(registrar) {
            this@registerCompilerExtensions.registerExtensions(configuration)
        }
    }
}
