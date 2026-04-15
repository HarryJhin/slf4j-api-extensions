// WITH_STDLIB
// FULL_JDK

fun box(): String {
    val inner = Outer.Inner()
    inner.doWork()
    return "OK"
}

class Outer {
    class Inner {
        fun doWork() {
            debug { "nested class logging" }
        }
    }
}
