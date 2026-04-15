package io.github.harryjhin.slf4j.extensions

import org.slf4j.Logger
import org.slf4j.Marker

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
