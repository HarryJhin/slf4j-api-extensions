package io.github.harryjhin.slf4j.extension

import kotlin.test.Test

class Slf4jExtensionsTest {

    data object Data {
        fun log() {
            trace { "Data log" }
        }
    }

    @Test
    fun removeCache() {
        Data.log()
        trace { "removeCache test" }
    }

    @Test
    fun trace() {
        trace { "trace test" }
    }

    @Test
    fun traceWithException() {
        trace(RuntimeException()) { "trace test" }
    }

    @Test
    fun debug() {
        debug { "debug test" }
    }

    @Test
    fun debugWithException() {
        debug(RuntimeException()) { "debug test" }
    }

    @Test
    fun info() {
        info { "info test" }
    }

    @Test
    fun infoWithException() {
        info(RuntimeException()) { "info test" }
    }

    @Test
    fun warn() {
        warn { "warn test" }
    }

    @Test
    fun warnWithException() {
        warn(RuntimeException()) { "warn test" }
    }

    @Test
    fun error() {
        error { "error test" }
    }

    @Test
    fun errorWithException() {
        error(RuntimeException()) { "error test" }
    }
}