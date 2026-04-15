package io.github.harryjhin.slf4j.extensions.gradle

open class Slf4jExtensionsGradleExtension {
    internal val myAnnotations = mutableListOf<String>()
    internal val myPackages = mutableListOf<String>()

    open var propertyName: String = "log"
    open var allClasses: Boolean = true

    open fun annotation(fqName: String) {
        myAnnotations.add(fqName)
    }

    open fun annotations(vararg fqNames: String) {
        myAnnotations.addAll(fqNames)
    }

    open fun packages(vararg names: String) {
        myPackages.addAll(names)
    }
}
