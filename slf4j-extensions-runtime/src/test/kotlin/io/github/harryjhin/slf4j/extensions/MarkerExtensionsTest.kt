package io.github.harryjhin.slf4j.extensions

import com.github.valfirst.slf4jtest.TestLoggerFactory
import com.github.valfirst.slf4jtest.TestLoggerFactoryExtension
import org.junit.jupiter.api.extension.ExtendWith
import org.slf4j.MarkerFactory
import org.slf4j.event.Level
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse

@ExtendWith(TestLoggerFactoryExtension::class)
class MarkerExtensionsTest {

    private val logger = TestLoggerFactory.getTestLogger("MarkerTestLogger")
    private val AUDIT = MarkerFactory.getMarker("AUDIT")

    @Test
    fun `trace with marker logs message`() {
        logger.trace(AUDIT) { "audit trace" }
        val events = logger.loggingEvents
        assertEquals(1, events.size)
        assertEquals(Level.TRACE, events[0].level)
        assertEquals("audit trace", events[0].message)
    }

    @Test
    fun `trace with marker and throwable logs both`() {
        val exception = RuntimeException("boom")
        logger.trace(AUDIT, exception) { "audit trace error" }
        val events = logger.loggingEvents
        assertEquals(1, events.size)
        assertEquals("audit trace error", events[0].message)
        assertEquals(exception, events[0].throwable.orElse(null))
    }

    @Test
    fun `trace with marker does not evaluate lambda when disabled`() {
        logger.setEnabledLevels(Level.ERROR)
        var evaluated = false
        logger.trace(AUDIT) { evaluated = true; "should not appear" }
        assertFalse(evaluated)
    }

    @Test
    fun `info with marker logs message`() {
        logger.info(AUDIT) { "audit info" }
        val events = logger.loggingEvents
        assertEquals(1, events.size)
        assertEquals(Level.INFO, events[0].level)
        assertEquals("audit info", events[0].message)
    }

    @Test
    fun `debug with marker logs message`() {
        logger.debug(AUDIT) { "audit debug" }
        val events = logger.loggingEvents
        assertEquals(1, events.size)
        assertEquals(Level.DEBUG, events[0].level)
        assertEquals("audit debug", events[0].message)
    }

    @Test
    fun `debug with marker and throwable logs both`() {
        val exception = RuntimeException("boom")
        logger.debug(AUDIT, exception) { "audit debug error" }
        val events = logger.loggingEvents
        assertEquals(1, events.size)
        assertEquals("audit debug error", events[0].message)
        assertEquals(exception, events[0].throwable.orElse(null))
    }

    @Test
    fun `debug with marker does not evaluate lambda when disabled`() {
        logger.setEnabledLevels(Level.ERROR)
        var evaluated = false
        logger.debug(AUDIT) { evaluated = true; "should not appear" }
        assertFalse(evaluated)
    }

    @Test
    fun `info with marker and throwable logs both`() {
        val exception = RuntimeException("boom")
        logger.info(AUDIT, exception) { "audit info error" }
        val events = logger.loggingEvents
        assertEquals(1, events.size)
        assertEquals("audit info error", events[0].message)
        assertEquals(exception, events[0].throwable.orElse(null))
    }

    @Test
    fun `info with marker does not evaluate lambda when disabled`() {
        logger.setEnabledLevels(Level.ERROR)
        var evaluated = false
        logger.info(AUDIT) { evaluated = true; "should not appear" }
        assertFalse(evaluated)
    }

    @Test
    fun `warn with marker logs message`() {
        logger.warn(AUDIT) { "audit warn" }
        val events = logger.loggingEvents
        assertEquals(1, events.size)
        assertEquals(Level.WARN, events[0].level)
        assertEquals("audit warn", events[0].message)
    }

    @Test
    fun `warn with marker and throwable logs both`() {
        val exception = RuntimeException("boom")
        logger.warn(AUDIT, exception) { "audit warn error" }
        val events = logger.loggingEvents
        assertEquals(1, events.size)
        assertEquals("audit warn error", events[0].message)
        assertEquals(exception, events[0].throwable.orElse(null))
    }

    @Test
    fun `warn with marker does not evaluate lambda when disabled`() {
        logger.setEnabledLevels(Level.ERROR)
        var evaluated = false
        logger.warn(AUDIT) { evaluated = true; "should not appear" }
        assertFalse(evaluated)
    }

    @Test
    fun `error with marker logs message`() {
        logger.error(AUDIT) { "audit error" }
        val events = logger.loggingEvents
        assertEquals(1, events.size)
        assertEquals(Level.ERROR, events[0].level)
        assertEquals("audit error", events[0].message)
    }

    @Test
    fun `error with marker and throwable logs both`() {
        val exception = RuntimeException("boom")
        logger.error(AUDIT, exception) { "audit error with throwable" }
        val events = logger.loggingEvents
        assertEquals(1, events.size)
        assertEquals(Level.ERROR, events[0].level)
        assertEquals("audit error with throwable", events[0].message)
        assertEquals(exception, events[0].throwable.orElse(null))
    }

    @Test
    fun `error with marker does not evaluate lambda when disabled`() {
        logger.setEnabledLevels()
        var evaluated = false
        logger.error(AUDIT) { evaluated = true; "should not appear" }
        assertFalse(evaluated)
    }
}
