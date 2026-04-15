package io.github.harryjhin.slf4j.extensions.compiler.runners

import io.github.harryjhin.slf4j.extensions.compiler.services.configurePlugin
import org.jetbrains.kotlin.test.FirParser
import org.jetbrains.kotlin.test.builders.TestConfigurationBuilder
import org.jetbrains.kotlin.test.directives.CodegenTestDirectives
import org.jetbrains.kotlin.test.runners.codegen.AbstractFirBlackBoxCodegenTestBase
import org.jetbrains.kotlin.test.services.EnvironmentBasedStandardLibrariesPathProvider
import org.jetbrains.kotlin.test.services.KotlinStandardLibrariesPathProvider

open class AbstractBoxTest : AbstractFirBlackBoxCodegenTestBase(FirParser.LightTree) {

    override fun createKotlinStandardLibrariesPathProvider(): KotlinStandardLibrariesPathProvider {
        return EnvironmentBasedStandardLibrariesPathProvider
    }

    override fun configure(builder: TestConfigurationBuilder) {
        super.configure(builder)
        with(builder) {
            defaultDirectives {
                +CodegenTestDirectives.IGNORE_DEXING
            }
            configurePlugin()
        }
    }
}
