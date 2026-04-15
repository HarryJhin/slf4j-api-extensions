// WITH_STDLIB

fun box(): String {
    val service = MyService()
    service.doWork()
    return "OK"
}

class MyService {
    fun doWork() {
        trace { "hello from trace" }
    }
}
