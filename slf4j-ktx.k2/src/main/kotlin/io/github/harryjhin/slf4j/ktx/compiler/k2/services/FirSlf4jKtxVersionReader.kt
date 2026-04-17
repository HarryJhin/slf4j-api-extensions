package io.github.harryjhin.slf4j.ktx.compiler.k2.services

import io.github.harryjhin.slf4j.ktx.compiler.Slf4jKtxVersions
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.extensions.FirExtensionSessionComponent

/**
 * K2 reader for `slf4j-ktx-core`'s compatibility manifest, backed by the session component
 * infrastructure so the lookup is cached per compiler session.
 *
 * Kotlin 1.9.25 caveat (see PLAN Appendix A): the FIR symbol → binary JAR bridge used by
 * kotlinx-serialization's K2 `FirVersionReader` (`markerClass.sourceElement`) is not exposed on
 * `FirClassLikeSymbol` in 1.9.25 — that accessor appeared in 2.x. For the v1 branch's Beta K2
 * path this reader therefore returns null (opting out of the K2 compatibility diagnostics); the
 * K1 `Slf4jKtxDeclarationChecker` covers the default compile path of this branch (K1 + language
 * version 1.9) where `KotlinJvmBinarySourceElement` is available.
 *
 * When the plugin moves to a Kotlin 2.x-paired release this class can adopt
 * `markerClass.sourceElement` unmodified.
 */
class FirSlf4jKtxVersionReader(session: FirSession) : FirExtensionSessionComponent(session) {

    @Suppress("UNUSED_PARAMETER")
    val runtimeVersions: Slf4jKtxVersions.RuntimeVersions? by lazy { null }
}

/** Session accessor — standard FirExtensionSessionComponent delegate idiom. */
val FirSession.slf4jKtxVersionReader: FirSlf4jKtxVersionReader by FirSession.sessionComponentAccessor()
