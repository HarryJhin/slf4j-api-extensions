// WITH_STDLIB
// FULL_JDK

import io.github.harryjhin.slf4j.ktx.Slf4j

fun box(): String {
    MyService().doWork()
    return "OK"
}

@Slf4j
class MyService {
    fun doWork() {
        info { "hello from info" }
    }
}
