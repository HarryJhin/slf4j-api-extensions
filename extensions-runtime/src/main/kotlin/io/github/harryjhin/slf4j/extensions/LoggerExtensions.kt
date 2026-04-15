package io.github.harryjhin.slf4j.extensions

import org.slf4j.Logger

inline fun Logger.trace(message: () -> String) {
    if (isTraceEnabled) {
        trace(message())
    }
}

inline fun Logger.trace(throwable: Throwable, message: () -> String) {
    if (isTraceEnabled) {
        trace(message(), throwable)
    }
}

inline fun Logger.debug(message: () -> String) {
    if (isDebugEnabled) {
        debug(message())
    }
}

inline fun Logger.debug(throwable: Throwable, message: () -> String) {
    if (isDebugEnabled) {
        debug(message(), throwable)
    }
}
