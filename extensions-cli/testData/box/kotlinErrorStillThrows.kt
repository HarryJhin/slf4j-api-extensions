// WITH_STDLIB

fun box(): String {
    val service = MyService()
    return try {
        service.doWork()
        "Fail: should have thrown"
    } catch (e: IllegalStateException) {
        "OK"
    }
}

class MyService {
    fun doWork(): String {
        error("this should throw")
    }
}
