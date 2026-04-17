package io.github.harryjhin.slf4j.ktx

import org.slf4j.Logger
import org.slf4j.Marker

// Marker-qualified, lazy-lambda SLF4J overloads. The plain (no-marker) lazy-lambda API is not
// provided here — the slf4j-ktx compiler plugin generates equivalent Companion-member functions
// for @Slf4j-annotated classes instead. These Marker overloads complement that by covering the
// SLF4J Marker dimension, which the plugin does not synthesize.

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
