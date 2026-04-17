package io.github.harryjhin.slf4j.ktx.compiler.k1

import io.github.harryjhin.slf4j.ktx.compiler.Slf4jKtxConfig
import org.jetbrains.kotlin.container.StorageComponentContainer
import org.jetbrains.kotlin.container.useInstance
import org.jetbrains.kotlin.descriptors.ModuleDescriptor
import org.jetbrains.kotlin.extensions.StorageComponentContainerContributor
import org.jetbrains.kotlin.platform.TargetPlatform

/**
 * Bridges [Slf4jKtxDeclarationChecker] into K1's analysis pipeline. Config is captured here so
 * the checker consults the same trigger-annotation set as the synthesis extension.
 */
class Slf4jKtxPluginComponentContainerContributor(
    private val config: Slf4jKtxConfig,
) : StorageComponentContainerContributor {

    override fun registerModuleComponents(
        container: StorageComponentContainer,
        platform: TargetPlatform,
        moduleDescriptor: ModuleDescriptor,
    ) {
        container.useInstance(Slf4jKtxDeclarationChecker(config))
    }
}
