// WITH_STDLIB
// FULL_JDK

import io.github.harryjhin.slf4j.ktx.Slf4j

// Collision policy: when the Companion already has any callable named `info`, the plugin
// silent-skips *all* `info` overloads. Other level names are still generated.

fun box(): String {
    val r = Widget.infoLength("hello")
    if (r != 5) return "FAIL: $r"
    Widget().render()
    return "OK"
}

@Slf4j
class Widget {
    fun render() {
        // user's info() takes (String) → Int; plugin's `info(() -> String)` is NOT generated.
        val n = info("abc")
        if (n != 3) throw RuntimeException("plugin info overrode user info")
        // Other levels are plugin-generated.
        debug { "still works" }
    }

    companion object {
        fun infoLength(s: String): Int = info(s)
        fun info(s: String): Int = s.length
    }
}
