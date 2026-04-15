// WITH_STDLIB
// FULL_JDK

fun box(): String {
    val impl = MyImpl()
    impl.doWork()
    return "OK"
}

interface MyService {
    fun doWork()
}

class MyImpl : MyService {
    override fun doWork() {
        info { "working" }
    }
}
