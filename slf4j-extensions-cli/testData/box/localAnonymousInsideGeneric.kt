// WITH_STDLIB
// FULL_JDK

abstract class TypeRef<T>

object Utils {
    fun <T> headerConvert(list: List<T>?): String {
        val ref = object : TypeRef<Map<String, Any>>() {}
        return list?.firstOrNull()?.toString() ?: ref::class.java.simpleName
    }
}

fun box(): String {
    val result = Utils.headerConvert(listOf("hello"))
    return if (result == "hello") "OK" else "FAIL: $result"
}
