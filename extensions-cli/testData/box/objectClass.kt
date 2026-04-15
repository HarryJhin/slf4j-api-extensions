// WITH_STDLIB

fun box(): String {
    AppLogger.start()
    return "OK"
}

object AppLogger {
    fun start() {
        info { "application started" }
    }
}
