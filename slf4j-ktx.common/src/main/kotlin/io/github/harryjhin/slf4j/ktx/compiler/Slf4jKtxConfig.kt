package io.github.harryjhin.slf4j.ktx.compiler

import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.name.FqName

/**
 * Single config object threaded through K1, K2 and the IR backend — keeps the three sites
 * consulting the same trigger-annotation set. The default trigger is always `@Slf4j`; user-
 * supplied annotations (via CLI `annotation` option) are merged on top.
 */
data class Slf4jKtxConfig(val annotations: List<FqName>) {
    companion object {
        /**
         * Builds the config from a [CompilerConfiguration]. Merges the hard-coded default
         * (`@Slf4j`) with user-provided annotation FQNs and de-duplicates. Order is preserved
         * so the default wins in case of duplicates.
         */
        fun from(configuration: CompilerConfiguration): Slf4jKtxConfig {
            val user = configuration.get(Slf4jKtxConfigurationKeys.ANNOTATIONS).orEmpty()
            val all = (listOf(Slf4jKtxPluginNames.SLF4J_ANNOTATION_FQ_NAME.asString()) + user).distinct()
            return Slf4jKtxConfig(all.map(::FqName))
        }
    }
}
