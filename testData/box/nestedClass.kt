// WITH_STDLIB
// FULL_JDK

import io.github.harryjhin.slf4j.ktx.Slf4j

fun box(): String {
    Outer.Inner().doWork()
    return "OK"
}

class Outer {
    @Slf4j
    class Inner {
        fun doWork() {
            debug { "nested class logging" }
        }
    }
}
