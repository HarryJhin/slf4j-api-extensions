// WITH_STDLIB
// FULL_JDK

import io.github.harryjhin.slf4j.ktx.Slf4j

fun box(): String {
    Handler().handle(RuntimeException("boom"))
    return "OK"
}

@Slf4j
class Handler {
    fun handle(t: Throwable) {
        error(t) { "caught throwable" }
        warn(t) { "warning with cause" }
    }
}
