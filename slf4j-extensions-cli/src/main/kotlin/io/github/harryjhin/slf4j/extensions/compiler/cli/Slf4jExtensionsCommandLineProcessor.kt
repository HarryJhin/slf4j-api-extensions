package io.github.harryjhin.slf4j.extensions.compiler.cli

import io.github.harryjhin.slf4j.extensions.compiler.Slf4jExtensionsConfigurationKeys
import io.github.harryjhin.slf4j.extensions.compiler.Slf4jExtensionsPluginNames
import org.jetbrains.kotlin.compiler.plugin.AbstractCliOption
import org.jetbrains.kotlin.compiler.plugin.CliOption
import org.jetbrains.kotlin.compiler.plugin.CommandLineProcessor
import org.jetbrains.kotlin.compiler.plugin.ExperimentalCompilerApi
import org.jetbrains.kotlin.config.CompilerConfiguration

@OptIn(ExperimentalCompilerApi::class)
class Slf4jExtensionsCommandLineProcessor : CommandLineProcessor {

    override val pluginId: String = Slf4jExtensionsPluginNames.PLUGIN_ID

    override val pluginOptions: Collection<CliOption> = listOf(
        CliOption(
            optionName = Slf4jExtensionsPluginNames.PROPERTY_NAME_OPTION,
            valueDescription = "<name>",
            description = "Logger property name (default: log)",
            required = false,
        ),
        CliOption(
            optionName = Slf4jExtensionsPluginNames.ANNOTATION_OPTION,
            valueDescription = "<fqname>",
            description = "Annotation FQ name to target",
            required = false,
            allowMultipleOccurrences = true,
        ),
        CliOption(
            optionName = Slf4jExtensionsPluginNames.PACKAGES_OPTION,
            valueDescription = "<name>",
            description = "Package name to target",
            required = false,
            allowMultipleOccurrences = true,
        ),
        CliOption(
            optionName = Slf4jExtensionsPluginNames.ALL_CLASSES_OPTION,
            valueDescription = "<true|false>",
            description = "Apply to all classes (default: true)",
            required = false,
        ),
    )

    override fun processOption(
        option: AbstractCliOption,
        value: String,
        configuration: CompilerConfiguration,
    ) {
        when (option.optionName) {
            Slf4jExtensionsPluginNames.PROPERTY_NAME_OPTION ->
                configuration.put(Slf4jExtensionsConfigurationKeys.PROPERTY_NAME, value)
            Slf4jExtensionsPluginNames.ANNOTATION_OPTION ->
                configuration.appendList(Slf4jExtensionsConfigurationKeys.ANNOTATIONS, value)
            Slf4jExtensionsPluginNames.PACKAGES_OPTION ->
                configuration.appendList(Slf4jExtensionsConfigurationKeys.PACKAGES, value)
            Slf4jExtensionsPluginNames.ALL_CLASSES_OPTION ->
                configuration.put(Slf4jExtensionsConfigurationKeys.ALL_CLASSES, value.toBoolean())
        }
    }
}
