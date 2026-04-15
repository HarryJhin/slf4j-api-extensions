package io.github.harryjhin.slf4j.extensions.gradle

import org.gradle.api.Project
import org.gradle.api.provider.Provider
import org.jetbrains.kotlin.gradle.plugin.KotlinCompilation
import org.jetbrains.kotlin.gradle.plugin.KotlinCompilerPluginSupportPlugin
import org.jetbrains.kotlin.gradle.plugin.SubpluginArtifact
import org.jetbrains.kotlin.gradle.plugin.SubpluginOption

class Slf4jExtensionsGradlePlugin : KotlinCompilerPluginSupportPlugin {

    companion object {
        private const val EXTENSION_NAME = "slf4jExtensions"
        private const val PLUGIN_ID = "io.github.harryjhin.slf4j-extensions"
        private const val GROUP_ID = "io.github.harryjhin"
        private const val COMPILER_ARTIFACT_ID = "slf4j-extensions-compiler"
        private const val RUNTIME_ARTIFACT_ID = "slf4j-extensions-runtime"
    }

    override fun apply(target: Project) {
        target.extensions.create(EXTENSION_NAME, Slf4jExtensionsGradleExtension::class.java)
    }

    override fun isApplicable(kotlinCompilation: KotlinCompilation<*>): Boolean = true

    override fun applyToCompilation(
        kotlinCompilation: KotlinCompilation<*>,
    ): Provider<List<SubpluginOption>> {
        val project = kotlinCompilation.target.project
        val extension = project.extensions.getByType(Slf4jExtensionsGradleExtension::class.java)

        // Auto-add runtime dependency (version omitted — resolved via BOM or composite build)
        project.dependencies.add(
            "implementation",
            "$GROUP_ID:$RUNTIME_ARTIFACT_ID",
        )

        return project.provider {
            val options = mutableListOf<SubpluginOption>()

            options += SubpluginOption("propertyName", extension.propertyName)
            options += SubpluginOption("allClasses", extension.allClasses.toString())

            for (anno in extension.myAnnotations) {
                options += SubpluginOption("annotation", anno)
            }

            for (pkg in extension.myPackages) {
                options += SubpluginOption("package", pkg)
            }

            options
        }
    }

    override fun getCompilerPluginId(): String = PLUGIN_ID

    override fun getPluginArtifact(): SubpluginArtifact =
        SubpluginArtifact(GROUP_ID, COMPILER_ARTIFACT_ID)
}
