package io.github.harryjhin.slf4j.extensions.gradle

open class Slf4jExtensionsGradleExtension {
    internal val myAnnotations = mutableListOf<String>()
    internal val myExcludeAnnotations = mutableListOf<String>()
    internal val myPackages = mutableListOf<String>()

    open var propertyName: String = "log"
    open var allClasses: Boolean = true

    open fun annotation(fqName: String) {
        myAnnotations.add(fqName)
    }

    open fun annotations(vararg fqNames: String) {
        myAnnotations.addAll(fqNames)
    }

    /**
     * Skip classes carrying this annotation (including companion objects
     * of such classes). Typical use: exclude JPA entity classes that would
     * otherwise clash with Kotlin-aware annotation processors:
     *
     * ```kotlin
     * slf4jExtensions {
     *     excludeAnnotation("jakarta.persistence.Entity")
     *     excludeAnnotation("jakarta.persistence.MappedSuperclass")
     *     excludeAnnotation("jakarta.persistence.Embeddable")
     * }
     * ```
     */
    open fun excludeAnnotation(fqName: String) {
        myExcludeAnnotations.add(fqName)
    }

    open fun excludeAnnotations(vararg fqNames: String) {
        myExcludeAnnotations.addAll(fqNames)
    }

    open fun packages(vararg names: String) {
        myPackages.addAll(names)
    }
}
