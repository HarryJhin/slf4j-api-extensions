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
    fun `error with marker and throwable logs both`() {
        val exception = RuntimeException("boom")
        logger.error(AUDIT, exception) { "audit error" }
        val events = logger.loggingEvents
        assertEquals(1, events.size)
        assertEquals(Level.ERROR, events[0].level)
        assertEquals("audit error", events[0].message)
        assertEquals(exception, events[0].throwable.orElse(null))
    }
}
