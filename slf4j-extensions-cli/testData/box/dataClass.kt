// WITH_STDLIB
// FULL_JDK

fun box(): String {
    val event = Event("test")
    event.logIt()
    return "OK"
}

data class Event(val name: String) {
    fun logIt() {
        info { "event: $name" }
    }
}
