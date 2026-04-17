package io.github.harryjhin.slf4j.ktx.runners

import org.jetbrains.kotlin.test.builders.TestConfigurationBuilder
import org.jetbrains.kotlin.test.directives.CodegenTestDirectives
import org.jetbrains.kotlin.test.runners.codegen.AbstractIrBlackBoxCodegenTest
import org.jetbrains.kotlin.test.services.EnvironmentBasedStandardLibrariesPathProvider
import org.jetbrains.kotlin.test.services.KotlinStandardLibrariesPathProvider

/**
 * K1 box-test base: compiles each testData/box fixture through the K1 frontend + IR backend
 * with slf4j-ktx's Register-All extensions active, then executes `fun box()` and asserts the
 * return value is `"OK"`.
 *
 * Kotlin 1.9.25 keeps K1 as the default compile path, so this base covers the primary user
 * experience. K2 coverage via [AbstractSlf4jKtxFirLightTreeBoxTest].
 */
abstract class AbstractSlf4jKtxK1BoxTest : AbstractIrBlackBoxCodegenTest() {

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
