// WITH_STDLIB
// FULL_JDK

import io.github.harryjhin.slf4j.ktx.Slf4j

// 1-hop meta-annotation (SPEC §2.6, P8). @Slf4j on a user annotation makes that annotation
// itself a trigger. The predicate is `annotated or metaAnnotated(..., includeItself = false)`.

@Slf4j
annotation class LoggedStereotype

@LoggedStereotype
class OrderService {
    fun process() {
        info { "processing" }
    }
}

fun box(): String {
    OrderService().process()
    return "OK"
}
