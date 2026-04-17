package io.github.harryjhin.slf4j.ktx.compiler

import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name

/**
 * SLF4J and Kotlin-stdlib symbols referenced by the plugin. Named `EntityNames` after the
 * kotlinx-serialization equivalent (`SerialEntityNames`).
 */
object Slf4jKtxEntityNames {
    /** `org.slf4j.Logger` — the type of the synthetic `log` property. */
    val LOGGER_CLASS_ID: ClassId = ClassId(FqName("org.slf4j"), Name.identifier("Logger"))

    /** `org.slf4j.LoggerFactory` — owner of `getLogger(String): Logger` used in the initializer. */
    val LOGGER_FACTORY_CLASS_ID: ClassId = ClassId(FqName("org.slf4j"), Name.identifier("LoggerFactory"))

    /** Method name on LoggerFactory that the plugin calls. */
    val GET_LOGGER_NAME: Name = Name.identifier("getLogger")

    /** Fixed name of the Companion/object property injected by the plugin. */
    val LOG_PROPERTY_ID: Name = Name.identifier(Slf4jKtxPluginNames.LOG_PROPERTY_NAME)

    /** Set form of [LOG_PROPERTY_ID] — every synthetic declaration the plugin contributes. */
    val ALL_CALLABLE_NAMES: Set<Name> = setOf(LOG_PROPERTY_ID)

    /**
     * Package hosting `T.trace/.debug/.info/.warn/.error × 2` runtime extensions in
     * `slf4j-ktx-core`. The IR call-site rewriter matches this package + one of
     * [LOG_LEVEL_NAMES] to identify a call it should redirect to the enclosing class's
     * `Companion.log`.
     */
    val LOGGER_EXTENSIONS_PACKAGE: FqName = FqName("io.github.harryjhin.slf4j.ktx")

    /** Level function names the runtime library exposes as `T.<level>(...)` extensions. */
    val LOG_LEVEL_NAMES: List<Name> = listOf("trace", "debug", "info", "warn", "error").map(Name::identifier)
}
