package io.github.harryjhin.slf4j.ktx

import org.slf4j.MDC
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertFailsWith

class MdcExtensionsTest {

    @Test
    fun `withMDC sets value during block`() {
        assertNull(MDC.get("requestId"))
        withMDC("requestId" to "abc-123") {
            assertEquals("abc-123", MDC.get("requestId"))
        }
    }

    @Test
    fun `withMDC removes value after block`() {
        withMDC("key" to "value") {
            assertEquals("value", MDC.get("key"))
        }
        assertNull(MDC.get("key"))
    }

    @Test
    fun `withMDC restores previous value`() {
        MDC.put("key", "original")
        try {
            withMDC("key" to "temporary") {
                assertEquals("temporary", MDC.get("key"))
            }
            assertEquals("original", MDC.get("key"))
        } finally {
            MDC.remove("key")
        }
    }

    @Test
    fun `withMDC restores on exception`() {
        MDC.put("key", "original")
        try {
            assertFailsWith<RuntimeException> {
                withMDC("key" to "temporary") {
                    throw RuntimeException("boom")
                }
            }
            assertEquals("original", MDC.get("key"))
        } finally {
            MDC.remove("key")
        }
    }

    @Test
    fun `withMDC supports multiple pairs`() {
        withMDC("a" to "1", "b" to "2", "c" to "3") {
            assertEquals("1", MDC.get("a"))
            assertEquals("2", MDC.get("b"))
            assertEquals("3", MDC.get("c"))
        }
        assertNull(MDC.get("a"))
        assertNull(MDC.get("b"))
        assertNull(MDC.get("c"))
    }

    @Test
    fun `withMDC with map sets and restores`() {
        val context = mapOf("userId" to "user-1", "traceId" to "trace-1")
        withMDC(context) {
            assertEquals("user-1", MDC.get("userId"))
            assertEquals("trace-1", MDC.get("traceId"))
        }
        assertNull(MDC.get("userId"))
        assertNull(MDC.get("traceId"))
    }

    @Test
    fun `nested withMDC restores correctly`() {
        withMDC("key" to "outer") {
            assertEquals("outer", MDC.get("key"))
            withMDC("key" to "inner") {
                assertEquals("inner", MDC.get("key"))
            }
            assertEquals("outer", MDC.get("key"))
        }
        assertNull(MDC.get("key"))
    }

    @Test
    fun `withMDC returns block result`() {
        val result = withMDC("key" to "value") {
            42
        }
        assertEquals(42, result)
    }
}
