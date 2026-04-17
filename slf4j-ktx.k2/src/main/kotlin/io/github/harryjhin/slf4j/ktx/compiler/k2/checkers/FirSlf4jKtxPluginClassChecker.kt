package io.github.harryjhin.slf4j.ktx.compiler.k2.checkers

import io.github.harryjhin.slf4j.ktx.compiler.Slf4jKtxConfig
import io.github.harryjhin.slf4j.ktx.compiler.Slf4jKtxVersions
import io.github.harryjhin.slf4j.ktx.compiler.k2.FirSlf4jKtxClassFilter
import io.github.harryjhin.slf4j.ktx.compiler.k2.services.slf4jKtxVersionReader
import org.jetbrains.kotlin.config.KotlinCompilerVersion
import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.diagnostics.reportOn
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.checkers.declaration.FirClassChecker
import org.jetbrains.kotlin.fir.declarations.FirClass
import org.jetbrains.kotlin.fir.symbols.impl.FirRegularClassSymbol

/**
 * K2 class-level checker that surfaces `CORE_MISSING` / `CORE_TOO_OLD` / `COMPILER_TOO_OLD` at
 * user `@Slf4j` sites. Parallels `FirSerializationPluginClassChecker.checkVersions`.
 *
 * 1.9.25 note: FirClassChecker's no-arg super constructor is used — `MppCheckerKind` did not
 * exist yet (PLAN Appendix A).
 */
class FirSlf4jKtxPluginClassChecker(
    private val config: Slf4jKtxConfig,
) : FirClassChecker() {

    override fun check(declaration: FirClass, context: CheckerContext, reporter: DiagnosticReporter) {
        val session = context.session
        val symbol = declaration.symbol as? FirRegularClassSymbol ?: return
        if (!FirSlf4jKtxClassFilter.shouldGenerateFor(symbol, session, config)) return

        // On 1.9.25 the K2 version reader is a stub (FirClassLikeSymbol → binary JAR bridge not
        // exposed; see FirSlf4jKtxVersionReader). Silently skip when unavailable so we do not
        // surface a false CORE_MISSING on the K2 path; the K1 checker covers the primary compile
        // path of this branch.
        val versions = session.slf4jKtxVersionReader.runtimeVersions ?: return
        if (!versions.implementationVersionMatchSupported()) {
            reporter.reportOn(
                declaration.source,
                FirSlf4jKtxErrors.CORE_TOO_OLD,
                versions.implementationVersion?.toString() ?: "unknown",
                Slf4jKtxVersions.MINIMAL_SUPPORTED_VERSION.toString(),
                context,
            )
        }
        val currentCompilerRaw = KotlinCompilerVersion.getVersion() ?: return
        val currentCompiler = Slf4jKtxVersions.Version.parse(currentCompilerRaw) ?: return
        if (!versions.currentCompilerMatchRequired(currentCompiler)) {
            reporter.reportOn(
                declaration.source,
                FirSlf4jKtxErrors.COMPILER_TOO_OLD,
                currentCompiler.toString(),
                versions.requireKotlinVersion?.toString() ?: "unknown",
                context,
            )
        }
    }
}
