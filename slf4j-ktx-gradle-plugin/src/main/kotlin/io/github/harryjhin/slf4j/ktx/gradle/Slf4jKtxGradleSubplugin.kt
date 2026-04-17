package io.github.harryjhin.slf4j.ktx.gradle

import org.gradle.api.Project
import org.gradle.api.provider.Provider
import org.jetbrains.kotlin.gradle.plugin.KotlinCompilation
import org.jetbrains.kotlin.gradle.plugin.KotlinCompilerPluginSupportPlugin
import org.jetbrains.kotlin.gradle.plugin.SubpluginArtifact
import org.jetbrains.kotlin.gradle.plugin.SubpluginOption

/**
 * Main Gradle plugin. Wires [Slf4jKtxGradleExtension] into the target project, then contributes
 * one `annotation` [SubpluginOption] per user-registered FQN to every Kotlin compilation.
 *
 * Shape follows `SerializationGradleSubplugin` (2-arg [SubpluginArtifact] — plugin version is
 * paired with the Kotlin compiler version, so no explicit version is needed; the Kotlin Gradle
 * plugin resolves the compiler-plugin artifact from its own version).
 */
class Slf4jKtxGradleSubplugin : KotlinCompilerPluginSupportPlugin {

    override fun apply(target: Project) {
        target.extensions.create(EXTENSION_NAME, Slf4jKtxGradleExtension::class.java)
    }

    override fun isApplicable(kotlinCompilation: KotlinCompilation<*>): Boolean = true

    override fun getCompilerPluginId(): String = PLUGIN_ID

    /**
     * Release publication strategy: plugin version == Kotlin version, so the default 2-arg
     * SubpluginArtifact works (Kotlin Gradle plugin fills the version from the consumer's Kotlin
     * version). For SNAPSHOT publications that decoupling breaks — `1.9.25-SNAPSHOT` is not a
     * Kotlin version — so we pass the version explicitly. The plugin version is stamped into a
     * classpath resource at build time (see `generatePluginVersionProperties` in build.gradle.kts).
     */
    override fun getPluginArtifact(): SubpluginArtifact {
        val version = PLUGIN_VERSION
        return if (version.endsWith("-SNAPSHOT")) {
            SubpluginArtifact(GROUP_NAME, ARTIFACT_NAME, version)
        } else {
            SubpluginArtifact(GROUP_NAME, ARTIFACT_NAME)
        }
    }

    override fun applyToCompilation(
        kotlinCompilation: KotlinCompilation<*>,
    ): Provider<List<SubpluginOption>> {
        val project = kotlinCompilation.target.project
        val extension = project.extensions.getByType(Slf4jKtxGradleExtension::class.java)
        return project.provider {
            extension.myAnnotations.map { SubpluginOption(ANNOTATION_OPTION, it) }
        }
    }

    companion object {
        const val EXTENSION_NAME: String = "slf4jKtx"
        const val PLUGIN_ID: String = "io.github.harryjhin.slf4j-ktx"
        const val GROUP_NAME: String = "io.github.harryjhin"
        const val ARTIFACT_NAME: String = "slf4j-ktx-compiler-plugin-embeddable"
        const val ANNOTATION_OPTION: String = "annotation"

        private const val VERSION_RESOURCE = "/io/github/harryjhin/slf4j/ktx/gradle/version.properties"

        private val PLUGIN_VERSION: String by lazy {
            val stream = Slf4jKtxGradleSubplugin::class.java.getResourceAsStream(VERSION_RESOURCE)
                ?: error("slf4j-ktx Gradle plugin missing $VERSION_RESOURCE — rebuild required")
            stream.use { input ->
                val props = java.util.Properties().apply { load(input) }
                props.getProperty("version")
                    ?: error("version not found in $VERSION_RESOURCE")
            }
        }
    }
}
