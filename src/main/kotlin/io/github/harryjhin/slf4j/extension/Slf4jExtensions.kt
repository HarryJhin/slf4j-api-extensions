package io.github.harryjhin.slf4j.extension

import io.github.harryjhin.slf4j.extension.Slf4jKotlinExtensionConfiguration.logger

/**
 * Log a message at the TRACE level.
 *
 * For more information, see [Documentation](https://harryjhin.github.io/slf4j-api-extensions/slf4j-api-extensions/io.github.harryjhin.slf4j.extensiontrace.html).
 *
 * @param message the message string to be logged
 * @since 1.0.0
 * @sample io.github.harryjhin.slf4j.extension.Slf4jExtensionsTest.trace
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
 * For more information, see [Documentation](https://harryjhin.github.io/slf4j-api-extensions/slf4j-api-extensions/io.github.harryjhin.slf4j.extensiontrace.html).
 *
 * @param throwable the exception (throwable) to log
 * @param message the message accompanying the exception
 * @since 1.0.0
 * @sample io.github.harryjhin.slf4j.extension.Slf4jExtensionsTest.traceWithException
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
 * For more information, see [Documentation](https://harryjhin.github.io/slf4j-api-extensions/slf4j-api-extensions/io.github.harryjhin.slf4j.extension/debug.html).
 *
 * @param message the message string to be logged
 * @since 1.0.0
 * @sample io.github.harryjhin.slf4j.extension.Slf4jExtensionsTest.debug
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
 * For more information, see [Documentation](https://harryjhin.github.io/slf4j-api-extensions/slf4j-api-extensions/io.github.harryjhin.slf4j.extensiondebug.html).
 *
 * @param throwable the exception (throwable) to log
 * @param message the message accompanying the exception
 * @since 1.0.0
 * @sample io.github.harryjhin.slf4j.extension.Slf4jExtensionsTest.debugWithException
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
 * For more information, see [Documentation](https://harryjhin.github.io/slf4j-api-extensions/slf4j-api-extensions/io.github.harryjhin.slf4j.extensioninfo.html).
 *
 * @param message the message string to be logged
 * @since 1.0.0
 * @sample io.github.harryjhin.slf4j.extension.Slf4jExtensionsTest.info
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
 * For more information, see [Documentation](https://harryjhin.github.io/slf4j-api-extensions/slf4j-api-extensions/io.github.harryjhin.slf4j.extensioninfo.html).
 *
 * @param throwable the exception (throwable) to log
 * @param message the message accompanying the exception
 * @since 1.0.0
 * @sample io.github.harryjhin.slf4j.extension.Slf4jExtensionsTest.infoWithException
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
 * For more information, see [Documentation](https://harryjhin.github.io/slf4j-api-extensions/slf4j-api-extensions/io.github.harryjhin.slf4j.extensionwarn.html).
 *
 * @param message the message string to be logged
 * @since 1.0.0
 * @sample io.github.harryjhin.slf4j.extension.Slf4jExtensionsTest.warn
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
 * For more information, see [Documentation](https://harryjhin.github.io/slf4j-api-extensions/slf4j-api-extensions/io.github.harryjhin.slf4j.extensionwarn.html).
 *
 * @param throwable the exception (throwable) to log
 * @param message the message accompanying the exception
 * @since 1.0.0
 * @sample io.github.harryjhin.slf4j.extension.Slf4jExtensionsTest.warnWithException
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
 * For more information, see [Documentation](https://harryjhin.github.io/slf4j-api-extensions/slf4j-api-extensions/io.github.harryjhin.slf4j.extensionerror.html).
 *
 * @param message the message string to be logged
 * @since 1.0.0
 * @sample io.github.harryjhin.slf4j.extension.Slf4jExtensionsTest.error
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
 * For more information, see [Documentation](https://harryjhin.github.io/slf4j-api-extensions/slf4j-api-extensions/io.github.harryjhin.slf4j.extensionerror.html).
 *
 * @param throwable the exception (throwable) to log
 * @param message the message accompanying the exception
 * @since 1.0.0
 * @sample io.github.harryjhin.slf4j.extension.Slf4jExtensionsTest.errorWithException
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
