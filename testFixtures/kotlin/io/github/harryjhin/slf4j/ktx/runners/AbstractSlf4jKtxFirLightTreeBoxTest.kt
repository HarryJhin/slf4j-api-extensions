package io.github.harryjhin.slf4j.ktx.runners

import org.jetbrains.kotlin.test.builders.TestConfigurationBuilder
import org.jetbrains.kotlin.test.directives.CodegenTestDirectives
import org.jetbrains.kotlin.test.runners.codegen.AbstractFirLightTreeBlackBoxCodegenTest
import org.jetbrains.kotlin.test.services.EnvironmentBasedStandardLibrariesPathProvider
import org.jetbrains.kotlin.test.services.KotlinStandardLibrariesPathProvider

/**
 * K2 box-test base: same fixtures as [AbstractSlf4jKtxK1BoxTest] but through the K2 FIR
 * frontend + IR backend. Register-All guarantees both frontends see the plugin; this base
 * flips the compiler's active frontend to FIR for each fixture.
 */
abstract class AbstractSlf4jKtxFirLightTreeBoxTest : AbstractFirLightTreeBlackBoxCodegenTest() {

    override fun createKotlinStandardLibrariesPathProvider(): KotlinStandardLibrariesPathProvider =
        EnvironmentBasedStandardLibrariesPathProvider

    override fun configure(builder: TestConfigurationBuilder) {
        super.configure(builder)
        with(builder) {
            defaultDirectives { +CodegenTestDirectives.IGNORE_DEXING }
            useConfigurators(::Slf4jKtxEnvironmentConfigurator)
            useCustomRuntimeClasspathProviders(::Slf4jKtxRuntimeClasspathProvider)
        }
    }
}
