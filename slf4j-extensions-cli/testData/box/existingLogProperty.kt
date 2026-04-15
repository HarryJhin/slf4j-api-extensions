// WITH_STDLIB
// FULL_JDK

fun box(): String {
    val service = MyService()
    return if (service.hasCustomLog()) "OK" else "Fail"
}

class MyService {
    private val log: String = "custom"

    fun hasCustomLog(): Boolean {
        return log == "custom"
    }
}
