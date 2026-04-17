// WITH_STDLIB
// FULL_JDK

import io.github.harryjhin.slf4j.ktx.Slf4j
import org.slf4j.Logger
import org.slf4j.LoggerFactory

// Strategy A (SPEC §8.1 collision policy): user declares `log: Logger` on the Companion.
// Plugin silent-skips `log` synthesis and reuses the user's Logger for the generated level
// functions (no compilation failure; generated bodies call `log.isInfoEnabled` etc.).
// Verifies the plugin respects a user-chosen logger name while still providing the lazy-lambda API.

fun box(): String {
    Widget().render()
    return "OK"
}

@Slf4j
class Widget {
    fun render() {
        info { "from custom logger" }
    }

    companion object {
        val log: Logger = LoggerFactory.getLogger("my.custom.Widget")
    }
}
