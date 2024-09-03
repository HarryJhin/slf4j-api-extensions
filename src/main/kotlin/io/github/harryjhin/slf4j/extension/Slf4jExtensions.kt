package io.github.harryjhin.slf4j.extension

import io.github.harryjhin.slf4j.extension.Slf4jKotlinExtensionConfiguration.logger

/**
 * Log a message at the TRACE level.
 *
 * @param message the message string to be logged
 * @since 1.0.0
 */
fun <T : Any> T.trace(
    message: () -> String,
) {
    with(logger) {
        if (isTraceEnabled) {
            trace(message())
        }
    }
}

/**
 * Log an exception (throwable) at the TRACE level with an accompanying message.
 *
 * @param throwable the exception (throwable) to log
 * @param message the message accompanying the exception
 * @since 1.0.0
 */
fun <T : Any> T.trace(
    throwable: Throwable,
    message: () -> String,
) {
    with(logger) {
        if (isTraceEnabled) {
            trace(message(), throwable)
        }
    }
}

/**
 * Log a message at the DEBUG level.
 *
 * @param message the message string to be logged
 * @since 1.0.0
 */
fun <T : Any> T.debug(
    message: () -> String,
) {
    with(logger) {
        if (isDebugEnabled) {
            debug(message())
        }
    }
}

/**
 * Log an exception (throwable) at the DEBUG level with an accompanying message.
 *
 * @param throwable the exception (throwable) to log
 * @param message the message accompanying the exception
 * @since 1.0.0
 */
fun <T : Any> T.debug(
    throwable: Throwable,
    message: () -> String,
) {
    with(logger) {
        if (isDebugEnabled) {
            debug(message(), throwable)
        }
    }
}

/**
 * Log a message at the INFO level.
 *
 * @param message the message string to be logged
 * @since 1.0.0
 */
fun <T : Any> T.info(
    message: () -> String,
) {
    with(logger) {
        if (isInfoEnabled) {
            info(message())
        }
    }
}

/**
 * Log an exception (throwable) at the INFO level with an accompanying message.
 *
 * @param throwable the exception (throwable) to log
 * @param message the message accompanying the exception
 * @since 1.0.0
 */
fun <T : Any> T.info(
    throwable: Throwable,
    message: () -> String,
) {
    with(logger) {
        if (isInfoEnabled) {
            info(message(), throwable)
        }
    }
}

/**
 * Log a message at the WARN level.
 *
 * @param message the message string to be logged
 * @since 1.0.0
 */
fun <T : Any> T.warn(
    message: () -> String,
) {
    with(logger) {
        if (isWarnEnabled) {
            warn(message())
        }
    }
}

/**
 * Log an exception (throwable) at the WARN level with an accompanying message.
 *
 * @param throwable the exception (throwable) to log
 * @param message the message accompanying the exception
 * @since 1.0.0
 */
fun <T : Any> T.warn(
    throwable: Throwable,
    message: () -> String,
) {
    with(logger) {
        if (isWarnEnabled) {
            warn(message(), throwable)
        }
    }
}

/**
 * Log a message at the ERROR level.
 *
 * @param message the message string to be logged
 * @since 1.0.0
 */
fun <T : Any> T.error(
    message: () -> String,
) {
    with(logger) {
        if (isErrorEnabled) {
            error(message())
        }
    }
}

/**
 * Log an exception (throwable) at the ERROR level with an accompanying message.
 *
 * @param throwable the exception (throwable) to log
 * @param message the message accompanying the exception
 * @since 1.0.0
 */
fun <T : Any> T.error(
    throwable: Throwable,
    message: () -> String,
) {
    with(logger) {
        if (isErrorEnabled) {
            error(message(), throwable)
        }
    }
}