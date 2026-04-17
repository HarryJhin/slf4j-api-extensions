package io.github.harryjhin.slf4j.ktx.compiler.cli

import io.github.harryjhin.slf4j.ktx.compiler.Slf4jKtxConfigurationKeys
import io.github.harryjhin.slf4j.ktx.compiler.Slf4jKtxPluginNames
import org.jetbrains.kotlin.compiler.plugin.AbstractCliOption
import org.jetbrains.kotlin.compiler.plugin.CliOption
import org.jetbrains.kotlin.compiler.plugin.CliOptionProcessingException
import org.jetbrains.kotlin.compiler.plugin.CommandLineProcessor
import org.jetbrains.kotlin.compiler.plugin.ExperimentalCompilerApi
import org.jetbrains.kotlin.config.CompilerConfiguration

/**
 * CLI options for slf4j-ktx. Only one option is exposed — additional trigger annotation FQNs.
 * Matches kotlinx-serialization's one-option minimalism (theirs is `disableIntrinsic`; ours is
 * `annotation` because the plugin is fundamentally driven by which annotations fire it, and
 * there is no useful knob beyond that list).
 *
 * `allowMultipleOccurrences = true` lets Gradle contribute a SubpluginOption per annotation, so
 * values accumulate rather than overwriting.
 */
@OptIn(ExperimentalCompilerApi::class)
class Slf4jKtxPluginOptions : CommandLineProcessor {

    override val pluginId: String = Slf4jKtxPluginNames.PLUGIN_ID

    override val pluginOptions: Collection<CliOption> = listOf(
        CliOption(
            optionName = Slf4jKtxPluginNames.ANNOTATION_OPTION,
            valueDescription = "fully-qualified annotation name",
            description = "Trigger annotation FQN. Multiple occurrences allowed; merged with the built-in `@Slf4j`.",
            required = false,
            allowMultipleOccurrences = true,
        ),
    )

    override fun processOption(
        option: AbstractCliOption,
        value: String,
        configuration: CompilerConfiguration,
    ) {
        when (option.optionName) {
            Slf4jKtxPluginNames.ANNOTATION_OPTION -> {
                val current = configuration.get(Slf4jKtxConfigurationKeys.ANNOTATIONS).orEmpty()
                configuration.put(Slf4jKtxConfigurationKeys.ANNOTATIONS, current + value)
            }
            else -> throw CliOptionProcessingException("Unknown option: ${option.optionName}")
        }
    }
}
