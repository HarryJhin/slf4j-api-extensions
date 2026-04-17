// WITH_STDLIB
// FULL_JDK

import io.github.harryjhin.slf4j.ktx.Slf4j

fun box(): String {
    Order(id = 1, name = "x").check()
    return "OK"
}

@Slf4j
data class Order(val id: Int, val name: String) {
    fun check() {
        debug { "order #$id ($name)" }
    }
}
