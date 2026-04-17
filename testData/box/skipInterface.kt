// WITH_STDLIB
// FULL_JDK

import io.github.harryjhin.slf4j.ktx.*

// @Slf4j on an interface must be ignored by the plugin. No Companion synthesis,
// no members generated. This file only verifies compilation does not break.

@Slf4j
interface Greeter {
    fun hello(): String
}

class Impl : Greeter {
    override fun hello(): String = "hi"
}

fun box(): String {
    val result = Impl().hello()
    if (result != "hi") return "FAIL: $result"
    return "OK"
}
