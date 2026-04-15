// WITH_STDLIB
// FULL_JDK

fun box(): String {
    val service = MyService()
    service.doWork()
    return "OK"
}

annotation class Logged

@Logged
class MyService {
    fun doWork() {
        info { "annotated service" }
    }
}
