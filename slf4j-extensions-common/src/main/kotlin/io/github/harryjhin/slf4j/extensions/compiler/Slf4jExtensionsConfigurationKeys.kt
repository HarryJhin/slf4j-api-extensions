package io.github.harryjhin.slf4j.extensions.compiler

import org.jetbrains.kotlin.config.CompilerConfigurationKey

object Slf4jExtensionsConfigurationKeys {
    val PROPERTY_NAME: CompilerConfigurationKey<String> =
        CompilerConfigurationKey.create("slf4j.extensions.propertyName")

    val ANNOTATIONS: CompilerConfigurationKey<List<String>> =
        CompilerConfigurationKey.create("slf4j.extensions.annotations")

    val EXCLUDE_ANNOTATIONS: CompilerConfigurationKey<List<String>> =
        CompilerConfigurationKey.create("slf4j.extensions.excludeAnnotations")

    val PACKAGES: CompilerConfigurationKey<List<String>> =
        CompilerConfigurationKey.create("slf4j.extensions.packages")

    val ALL_CLASSES: CompilerConfigurationKey<Boolean> =
        CompilerConfigurationKey.create("slf4j.extensions.allClasses")
}
