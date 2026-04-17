package io.github.harryjhin.slf4j.ktx

import org.slf4j.MDC

/**
 * Scoped MDC utilities: run [block] with additional Mapped Diagnostic Context entries in place
 * and restore the prior state on exit, including when [block] throws.
 *
 * Restore semantics: for each supplied key, the value present before the call is captured and
 * put back on return. Keys absent before the call are removed (not left behind as `null`).
 * This mirrors SLF4J's own `MDCCloseable` but works across value types and multiple keys in a
 * single call.
 *
 * ```
 * import io.github.harryjhin.slf4j.ktx.*
 *
 * withMDC("requestId" to requestId, "userId" to userId) {
 *     info { "processing" }     // requestId / userId are visible to the appender
 * }                             // original MDC restored even if `info { }` threw
 * ```
 */
inline fun <T> withMDC(vararg pairs: Pair<String, String>, block: () -> T): T {
    val previous = pairs.map { (key, _) -> key to MDC.get(key) }
    try {
        pairs.forEach { (key, value) -> MDC.put(key, value) }
        return block()
    } finally {
        previous.forEach { (key, oldValue) ->
            if (oldValue == null) {
                MDC.remove(key)
            } else {
                MDC.put(key, oldValue)
            }
        }
    }
}

/**
 * Map-accepting variant of [withMDC] — convenient when entries are built up programmatically.
 * Restore semantics are identical: previously-present keys are reinstated, previously-absent
 * keys are removed.
 */
inline fun <T> withMDC(context: Map<String, String>, block: () -> T): T {
    val previous = context.keys.associateWith { MDC.get(it) }
    try {
        context.forEach { (key, value) -> MDC.put(key, value) }
        return block()
    } finally {
        previous.forEach { (key, oldValue) ->
            if (oldValue == null) {
                MDC.remove(key)
            } else {
                MDC.put(key, oldValue)
            }
        }
    }
}
