// WITH_STDLIB
// FULL_JDK

import io.github.harryjhin.slf4j.ktx.Slf4j

fun box(): String {
    Emitter().emit()
    return "OK"
}

@Slf4j
class Emitter {
    fun emit() {
        trace { "t" }
        debug { "d" }
        info { "i" }
        warn { "w" }
        error { "e" }
    }
}
