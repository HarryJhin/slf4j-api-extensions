package io.github.harryjhin.slf4j.ktx

import org.slf4j.Logger
import org.slf4j.Marker

/**
 * Marker-qualified, lazy-lambda SLF4J overloads.
 *
 * These complement the plain (no-marker) `T.trace / T.debug / T.info / T.warn / T.error`
 * extensions in [LoggerExtensions.kt]. The slf4j-ktx compiler plugin handles the plain
 * variant — it rewrites those call sites on `@Slf4j`-annotated classes to hit the Companion's
 * pre-generated `log` field directly. Marker overloads are left as regular `Logger.xxx(...)`
 * receiver extensions because (a) a Marker argument changes the call shape so IR rewriting
 * buys less, and (b) invoking them via `log.trace(marker) { … }` from user code is the
 * natural call form.
 *
 * Each overload is a level-gated lazy invocation: the message lambda is evaluated only when
 * the corresponding `isXxxEnabled(marker)` returns true, matching the performance contract of
 * the plain variant.
 */

inline fun Logger.trace(marker: Marker, message: () -> String) {
    if (isTraceEnabled(marker)) {
        trace(marker, message())
    }
}

inline fun Logger.trace(marker: Marker, throwable: Throwable, message: () -> String) {
    if (isTraceEnabled(marker)) {
        trace(marker, message(), throwable)
    }
}

inline fun Logger.debug(marker: Marker, message: () -> String) {
    if (isDebugEnabled(marker)) {
        debug(marker, message())
    }
}

inline fun Logger.debug(marker: Marker, throwable: Throwable, message: () -> String) {
    if (isDebugEnabled(marker)) {
        debug(marker, message(), throwable)
    }
}

inline fun Logger.info(marker: Marker, message: () -> String) {
    if (isInfoEnabled(marker)) {
        info(marker, message())
    }
}

inline fun Logger.info(marker: Marker, throwable: Throwable, message: () -> String) {
    if (isInfoEnabled(marker)) {
        info(marker, message(), throwable)
    }
}

inline fun Logger.warn(marker: Marker, message: () -> String) {
    if (isWarnEnabled(marker)) {
        warn(marker, message())
    }
}

inline fun Logger.warn(marker: Marker, throwable: Throwable, message: () -> String) {
    if (isWarnEnabled(marker)) {
        warn(marker, message(), throwable)
    }
}

inline fun Logger.error(marker: Marker, message: () -> String) {
    if (isErrorEnabled(marker)) {
        error(marker, message())
    }
}

inline fun Logger.error(marker: Marker, throwable: Throwable, message: () -> String) {
    if (isErrorEnabled(marker)) {
        error(marker, message(), throwable)
    }
}
