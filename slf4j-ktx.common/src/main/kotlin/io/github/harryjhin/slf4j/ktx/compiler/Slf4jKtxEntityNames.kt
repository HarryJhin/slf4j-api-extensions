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

    /** Log-level function names the plugin synthesizes (both message-only and throwable-aware overloads). */
    val LOG_LEVEL_NAMES: List<Name> = listOf("trace", "debug", "info", "warn", "error").map(Name::identifier)

    /** Same as [LOG_LEVEL_NAMES] but as a Set for O(1) membership checks. */
    val LOG_LEVEL_NAME_SET: Set<Name> = LOG_LEVEL_NAMES.toSet()

    /** Union of [LOG_PROPERTY_ID] and [LOG_LEVEL_NAME_SET] — every callable the plugin contributes. */
    val ALL_CALLABLE_NAMES: Set<Name> = LOG_LEVEL_NAME_SET + LOG_PROPERTY_ID
}
