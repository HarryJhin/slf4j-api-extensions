package io.github.harryjhin.slf4j.ktx

import org.slf4j.LoggerFactory

/**
 * Level-named inline extension functions accepting a lazy message lambda.
 *
 * **Primary path (with compiler plugin)**: when the enclosing class carries a trigger
 * annotation (`@Slf4j`, a custom trigger, or a Spring stereotype), the compiler plugin
 * rewrites each call site in IR to fetch the pre-generated `Companion.log` backing field
 * directly. The default body below is never entered in that path; the emitted bytecode is a
 * single `GETSTATIC Foo$Companion.log` + `INVOKEVIRTUAL Logger.isXxxEnabled` + conditional
 * `INVOKEVIRTUAL Logger.xxx`. Zero allocation, zero reflection, zero map lookup per call.
 *
 * **Fallback path (without compiler plugin, or class without a trigger annotation)**: the
 * default body runs. SLF4J's own `LoggerFactory.getLogger` is backed by a ConcurrentHashMap
 * cache inside the binding, so the overhead is a single map lookup (typically <100 ns) plus
 * the `isXxxEnabled` guard. Correctness is preserved; only the microbenchmark-level peak
 * performance of a direct static-field read is sacrificed.
 *
 * IDE resolution: the extensions exist as real top-level declarations in this runtime
 * library, so every `info { "…" }` call resolves without a compiler plugin installed in
 * the IDE. That is the key difference from Companion-member synthesis, which depends on
 * IDE-side plugin support that is unavailable for third-party plugins.
 */

inline fun <reified T : Any> T.trace(message: () -> String) {
    val logger = LoggerFactory.getLogger(T::class.java)
    if (logger.isTraceEnabled) logger.trace(message())
}

inline fun <reified T : Any> T.trace(throwable: Throwable, message: () -> String) {
    val logger = LoggerFactory.getLogger(T::class.java)
    if (logger.isTraceEnabled) logger.trace(message(), throwable)
}

inline fun <reified T : Any> T.debug(message: () -> String) {
    val logger = LoggerFactory.getLogger(T::class.java)
    if (logger.isDebugEnabled) logger.debug(message())
}

inline fun <reified T : Any> T.debug(throwable: Throwable, message: () -> String) {
    val logger = LoggerFactory.getLogger(T::class.java)
    if (logger.isDebugEnabled) logger.debug(message(), throwable)
}

inline fun <reified T : Any> T.info(message: () -> String) {
    val logger = LoggerFactory.getLogger(T::class.java)
    if (logger.isInfoEnabled) logger.info(message())
}

inline fun <reified T : Any> T.info(throwable: Throwable, message: () -> String) {
    val logger = LoggerFactory.getLogger(T::class.java)
    if (logger.isInfoEnabled) logger.info(message(), throwable)
}

inline fun <reified T : Any> T.warn(message: () -> String) {
    val logger = LoggerFactory.getLogger(T::class.java)
    if (logger.isWarnEnabled) logger.warn(message())
}

inline fun <reified T : Any> T.warn(throwable: Throwable, message: () -> String) {
    val logger = LoggerFactory.getLogger(T::class.java)
    if (logger.isWarnEnabled) logger.warn(message(), throwable)
}

inline fun <reified T : Any> T.error(message: () -> String) {
    val logger = LoggerFactory.getLogger(T::class.java)
    if (logger.isErrorEnabled) logger.error(message())
}

inline fun <reified T : Any> T.error(throwable: Throwable, message: () -> String) {
    val logger = LoggerFactory.getLogger(T::class.java)
    if (logger.isErrorEnabled) logger.error(message(), throwable)
}
