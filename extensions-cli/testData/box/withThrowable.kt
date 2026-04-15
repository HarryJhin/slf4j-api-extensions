// WITH_STDLIB
// FULL_JDK

fun box(): String {
    val service = MyService()
    service.doWork()
    return "OK"
}

class MyService {
    fun doWork() {
        val ex = RuntimeException("boom")
        error(ex) { "failed with throwable" }
    }
}
