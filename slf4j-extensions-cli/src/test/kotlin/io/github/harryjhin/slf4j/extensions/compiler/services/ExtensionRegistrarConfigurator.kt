package io.github.harryjhin.slf4j.extensions.compiler.services

import io.github.harryjhin.slf4j.extensions.compiler.cli.Slf4jExtensionsCompilerPluginRegistrar
import org.jetbrains.kotlin.cli.jvm.config.addJvmClasspathRoot
import org.jetbrains.kotlin.compiler.plugin.CompilerPluginRegistrar
import org.jetbrains.kotlin.compiler.plugin.ExperimentalCompilerApi
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.test.builders.TestConfigurationBuilder
import org.jetbrains.kotlin.test.model.TestModule
import org.jetbrains.kotlin.test.services.EnvironmentConfigurator
import org.jetbrains.kotlin.test.services.RuntimeClasspathProvider
import org.jetbrains.kotlin.test.services.TestServices
import java.io.File

@OptIn(ExperimentalCompilerApi::class)
fun TestConfigurationBuilder.configurePlugin() {
    useConfigurators(::ExtensionRegistrarConfigurator)
    useCustomRuntimeClasspathProviders(::Slf4jRuntimeClasspathProvider)
}

@OptIn(ExperimentalCompilerApi::class)
class ExtensionRegistrarConfigurator(testServices: TestServices) : EnvironmentConfigurator(testServices) {

    override fun configureCompilerConfiguration(configuration: CompilerConfiguration, module: TestModule) {
        findJarContaining("org.slf4j.Logger")?.let {
            configuration.addJvmClasspathRoot(it)
        }
    }

    override fun CompilerPluginRegistrar.ExtensionStorage.registerCompilerExtensions(
        module: TestModule,
        configuration: CompilerConfiguration,
    ) {
        Slf4jExtensionsCompilerPluginRegistrar.registerExtensions(this, configuration)
    }
}

class Slf4jRuntimeClasspathProvider(testServices: TestServices) : RuntimeClasspathProvider(testServices) {
    override fun runtimeClassPaths(module: TestModule): List<File> {
        return listOfNotNull(findJarContaining("org.slf4j.Logger"))
    }
}

private fun findJarContaining(className: String): File? {
    val resourceName = className.replace('.', '/') + ".class"
    val url = ExtensionRegistrarConfigurator::class.java.classLoader.getResource(resourceName) ?: return null
    val urlString = url.toString()
    if (urlString.startsWith("jar:file:")) {
        val jarPath = urlString.removePrefix("jar:file:").substringBefore("!")
        return File(jarPath)
    }
    return null
}
