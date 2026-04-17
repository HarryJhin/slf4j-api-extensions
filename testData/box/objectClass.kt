// WITH_STDLIB
// FULL_JDK

import io.github.harryjhin.slf4j.ktx.*

fun box(): String {
    Registry.reload()
    return "OK"
}

@Slf4j
object Registry {
    fun reload() {
        info { "reload start" }
    }
}
