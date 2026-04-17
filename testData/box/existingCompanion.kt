// WITH_STDLIB
// FULL_JDK

import io.github.harryjhin.slf4j.ktx.*

fun box(): String {
    if (Widget.constantValue() != 42) return "FAIL: constant"
    Widget().render()
    return "OK"
}

@Slf4j
class Widget {
    fun render() {
        info { "render from widget" }
    }

    companion object {
        fun constantValue(): Int = 42
    }
}
