package io.github.harryjhin.slf4j.extensions

import org.slf4j.MDC

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
