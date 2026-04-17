package io.github.harryjhin.slf4j.ktx

import org.slf4j.MDC

/**
 * Execute [block] with MDC populated by [pairs] and restore the prior state on exit (including on
 * exception). Prior values are preserved and put back; keys that were absent before are removed.
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
 * Map-accepting variant of [withMDC]. Same restore semantics.
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
