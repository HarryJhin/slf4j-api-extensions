// WITH_STDLIB
// FULL_JDK

// Negative: MyService has no @Slf4j, so the plugin does not inject anything. The `error("…")`
// call resolves to `kotlin.error(message: Any): Nothing` and must throw IllegalStateException
// — verifying the plugin's `error(() -> String)` overload does not shadow stdlib on
// non-triggered classes.

fun box(): String {
    return try {
        MyService().doWork()
        "FAIL: should have thrown"
    } catch (e: IllegalStateException) {
        "OK"
    }
}

class MyService {
    fun doWork(): String {
        error("this should throw")
    }
}
