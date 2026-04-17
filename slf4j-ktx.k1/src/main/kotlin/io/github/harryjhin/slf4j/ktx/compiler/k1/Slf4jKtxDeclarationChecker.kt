package io.github.harryjhin.slf4j.ktx.compiler.k1

import io.github.harryjhin.slf4j.ktx.compiler.Slf4jKtxConfig
import io.github.harryjhin.slf4j.ktx.compiler.Slf4jKtxVersions
import org.jetbrains.kotlin.config.KotlinCompilerVersion
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.psi.KtDeclaration
import org.jetbrains.kotlin.resolve.checkers.DeclarationChecker
import org.jetbrains.kotlin.resolve.checkers.DeclarationCheckerContext
import org.jetbrains.kotlin.resolve.descriptorUtil.module

/**
 * K1 DeclarationChecker that surfaces compatibility diagnostics
 * ([Slf4jKtxPluginErrors.CORE_MISSING] / [Slf4jKtxPluginErrors.CORE_TOO_OLD] /
 * [Slf4jKtxPluginErrors.COMPILER_TOO_OLD]) on classes the plugin targets.
 *
 * Runs only for classes that pass [Slf4jKtxClassFilter] — the same filter that drives synthesis —
 * so diagnostics fire exactly on user-annotated sites.
 */
class Slf4jKtxDeclarationChecker(
    private val config: Slf4jKtxConfig,
) : DeclarationChecker {

    override fun check(
        declaration: KtDeclaration,
        descriptor: DeclarationDescriptor,
        context: DeclarationCheckerContext,
    ) {
        if (descriptor !is ClassDescriptor) return
        if (!Slf4jKtxClassFilter.shouldGenerateFor(descriptor, config)) return
        checkRuntimeVersion(declaration, descriptor, context)
    }

    private fun checkRuntimeVersion(
        declaration: KtDeclaration,
        descriptor: ClassDescriptor,
        context: DeclarationCheckerContext,
    ) {
        val versions = Slf4jKtxVersionReader.readRuntimeVersions(descriptor.module)
        if (versions == null) {
            context.trace.report(Slf4jKtxPluginErrors.CORE_MISSING.on(declaration))
            return
        }
        if (!versions.implementationVersionMatchSupported()) {
            context.trace.report(
                Slf4jKtxPluginErrors.CORE_TOO_OLD.on(
                    declaration,
                    versions.implementationVersion?.toString() ?: "unknown",
                    Slf4jKtxVersions.MINIMAL_SUPPORTED_VERSION.toString(),
                )
            )
        }
        val currentCompilerRaw = KotlinCompilerVersion.getVersion() ?: return
        val currentCompiler = Slf4jKtxVersions.Version.parse(currentCompilerRaw) ?: return
        if (!versions.currentCompilerMatchRequired(currentCompiler)) {
            context.trace.report(
                Slf4jKtxPluginErrors.COMPILER_TOO_OLD.on(
                    declaration,
                    currentCompiler.toString(),
                    versions.requireKotlinVersion?.toString() ?: "unknown",
                )
            )
        }
    }
}
