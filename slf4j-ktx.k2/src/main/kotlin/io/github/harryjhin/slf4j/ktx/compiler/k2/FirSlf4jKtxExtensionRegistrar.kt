package io.github.harryjhin.slf4j.ktx.compiler.k2

import io.github.harryjhin.slf4j.ktx.compiler.Slf4jKtxConfig
import io.github.harryjhin.slf4j.ktx.compiler.k2.checkers.FirSlf4jKtxCheckersComponent
import io.github.harryjhin.slf4j.ktx.compiler.k2.services.FirSlf4jKtxVersionReader
import org.jetbrains.kotlin.fir.analysis.extensions.FirAdditionalCheckersExtension
import org.jetbrains.kotlin.fir.extensions.FirDeclarationGenerationExtension
import org.jetbrains.kotlin.fir.extensions.FirExtensionRegistrar
import org.jetbrains.kotlin.fir.extensions.FirExtensionSessionComponent

/**
 * Registers all K2 extensions that make up `slf4j-ktx`:
 *  - [Slf4jKtxFirResolveExtension]         → declaration generation (Companion + members)
 *  - [FirSlf4jKtxCheckersComponent]        → compatibility diagnostics
 *  - [FirSlf4jKtxVersionReader]            → cached manifest read, consumed by the checker
 *
 * 1.9.25-only notes:
 *  - No `FirMetadataSerializerPlugin` (Kotlin 2.x+ API; K1 `DescriptorSerializerPlugin` covers
 *    the descriptor-metadata hook).
 *  - No `registerDiagnosticContainers` (Kotlin 2.x+ API; `FirSlf4jKtxErrors.init` self-registers
 *    the renderer via `RootDiagnosticRendererFactory`).
 */
class FirSlf4jKtxExtensionRegistrar(
    private val config: Slf4jKtxConfig,
) : FirExtensionRegistrar() {

    override fun ExtensionRegistrarContext.configurePlugin() {
        +FirDeclarationGenerationExtension.Factory { session ->
            Slf4jKtxFirResolveExtension(session, config)
        }
        +FirAdditionalCheckersExtension.Factory { session ->
            FirSlf4jKtxCheckersComponent(session, config)
        }
        +FirExtensionSessionComponent.Factory { session ->
            FirSlf4jKtxVersionReader(session)
        }
    }
}
