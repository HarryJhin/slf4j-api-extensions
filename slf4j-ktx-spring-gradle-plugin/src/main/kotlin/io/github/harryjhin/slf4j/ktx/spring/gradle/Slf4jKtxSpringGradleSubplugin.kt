package io.github.harryjhin.slf4j.ktx.spring.gradle

import io.github.harryjhin.slf4j.ktx.gradle.Slf4jKtxGradleExtension
import io.github.harryjhin.slf4j.ktx.gradle.Slf4jKtxGradleSubplugin
import org.gradle.api.Plugin
import org.gradle.api.Project

/**
 * Spring integration Gradle plugin. Follows the shape of
 * `libraries/tools/kotlin-allopen/src/common/kotlin/org/jetbrains/kotlin/allopen/gradle/KotlinSpringSubplugin.kt`:
 * a plain [Plugin] that auto-applies the main compiler-plugin support plugin and mutates its
 * extension. The compiler plugin is registered exactly once (by the main plugin); this plugin
 * only pushes the six Spring stereotype FQNs into [Slf4jKtxGradleExtension.myAnnotations].
 *
 * Users may still register additional triggers via `slf4jKtx { annotation("…") }`; entries
 * combine with the Spring FQNs pushed here.
 */
class Slf4jKtxSpringGradleSubplugin : Plugin<Project> {

    override fun apply(project: Project) {
        project.plugins.apply(Slf4jKtxGradleSubplugin::class.java)
        val extension = project.extensions.getByType(Slf4jKtxGradleExtension::class.java)
        SPRING_ANNOTATIONS.forEach { extension.annotation(it) }
    }

    companion object {
        const val SPRING_PLUGIN_ID: String = "io.github.harryjhin.slf4j-ktx.spring"

        val SPRING_ANNOTATIONS: List<String> = listOf(
            "org.springframework.stereotype.Component",
            "org.springframework.stereotype.Controller",
            "org.springframework.stereotype.Service",
            "org.springframework.stereotype.Repository",
            "org.springframework.web.bind.annotation.RestController",
            "org.springframework.web.bind.annotation.ControllerAdvice",
        )
    }
}
