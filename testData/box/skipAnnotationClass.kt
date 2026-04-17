// WITH_STDLIB
// FULL_JDK

import io.github.harryjhin.slf4j.ktx.*

// @Slf4j on an annotation class itself is ignored (not to be confused with the
// meta-annotation case, which is a user annotation carrying @Slf4j applied to a class).

@Slf4j
annotation class Marker

@Marker
class Holder

fun box(): String {
    Holder()
    return "OK"
}
