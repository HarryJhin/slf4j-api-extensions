package io.github.harryjhin.slf4j.extensions

import com.github.valfirst.slf4jtest.TestLoggerFactory
import com.github.valfirst.slf4jtest.TestLoggerFactoryExtension
import org.junit.jupiter.api.extension.ExtendWith
import org.slf4j.event.Level
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@ExtendWith(TestLoggerFactoryExtension::class)
class LoggerExtensionsTest {

    private val logger = TestLoggerFactory.getTestLogger("TestLogger")

    @Test
    fun `trace logs message when enabled`() {
        logger.trace { "trace message" }
        val events = logger.loggingEvents
        assertEquals(1, events.size)
        assertEquals(Level.TRACE, events[0].level)
        assertEquals("trace message", events[0].message)
    }

    @Test
    fun `trace with throwable logs message and exception`() {
        val exception = RuntimeException("boom")
        logger.trace(exception) { "trace error" }
        val events = logger.loggingEvents
        assertEquals(1, events.size)
        assertEquals("trace error", events[0].message)
        assertEquals(exception, events[0].throwable.orElse(null))
    }

    @Test
    fun `trace does not evaluate lambda when disabled`() {
        logger.setEnabledLevels(Level.ERROR)
        var evaluated = false
        logger.trace { evaluated = true; "should not appear" }
        assertFalse(evaluated)
        assertTrue(logger.loggingEvents.isEmpty())
    }

    @Test
    fun `debug logs message when enabled`() {
        logger.debug { "debug message" }
        val events = logger.loggingEvents
        assertEquals(1, events.size)
        assertEquals(Level.DEBUG, events[0].level)
        assertEquals("debug message", events[0].message)
    }

    @Test
    fun `debug with throwable logs message and exception`() {
        val exception = IllegalStateException("bad state")
        logger.debug(exception) { "debug error" }
        val events = logger.loggingEvents
        assertEquals(1, events.size)
        assertEquals("debug error", events[0].message)
        assertEquals(exception, events[0].throwable.orElse(null))
    }

    @Test
    fun `debug does not evaluate lambda when disabled`() {
        logger.setEnabledLevels(Level.ERROR)
        var evaluated = false
        logger.debug { evaluated = true; "should not appear" }
        assertFalse(evaluated)
        assertTrue(logger.loggingEvents.isEmpty())
    }
}
