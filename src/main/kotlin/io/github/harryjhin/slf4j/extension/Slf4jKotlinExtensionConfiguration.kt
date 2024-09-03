package io.github.harryjhin.slf4j.extension

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.util.Properties
import java.util.concurrent.ConcurrentHashMap

object Slf4jKotlinExtensionConfiguration {

    private val _logger: Logger = LoggerFactory.getLogger(Slf4jKotlinExtensionConfiguration::class.java)

    private val MAX_CACHE_SIZE: Int by lazy {
        Properties().apply {
            Slf4jKotlinExtensionConfiguration::class.java.getResourceAsStream("/slf4j-api-extensions.properties")
                ?.use { inputStream -> load(inputStream) }
        }.getProperty("max-cache-size")?.toIntOrNull() ?: 1_000
    }

    private val LOGGER_CACHE: ConcurrentHashMap<String, Logger> by lazy {
        ConcurrentHashMap<String, Logger>()
    }

    private val isFull: Boolean
        get() = LOGGER_CACHE.size >= MAX_CACHE_SIZE

    internal val <T : Any> T.logger: Logger
        get() = LOGGER_CACHE.getOrPut(this::class.java.name) { createLogger() }

    private fun <T : Any> T.createLogger(): Logger {
        if (isFull) {
            removeLogger()
        }
        val newLogger = LoggerFactory.getLogger(this::class.java)
        _logger.debug("[CREATE] Logger '${this::class.java.name}' is created.")

        return newLogger
    }

    private fun removeLogger() {
        synchronized(LOGGER_CACHE) {
            val key = LOGGER_CACHE.keys.firstOrNull() ?: return
            LOGGER_CACHE.remove(key)
            _logger.debug("[DELETE] Logger '${key}' is removed.")
        }
    }
}