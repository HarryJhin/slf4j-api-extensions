// WITH_STDLIB

fun box(): String {
    val service = MyService()
    service.doWork()
    return "OK"
}

class MyService {
    fun doWork() {
        trace { "trace" }
        debug { "debug" }
        info { "info" }
        warn { "warn" }
        error { "error" }
    }
}
