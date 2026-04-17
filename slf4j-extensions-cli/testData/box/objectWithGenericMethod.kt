// WITH_STDLIB
// FULL_JDK

fun box(): String {
    val result = ExcelUtils.headerConvert(listOf("a", "b"))
    ExcelUtils.logIt()
    return if (result == listOf("a", "b")) "OK" else "FAIL: $result"
}

object ExcelUtils {
    fun <T> headerConvert(list: List<T>?): List<String>? {
        return list?.map { it.toString() }
    }

    fun <T> dataListConvert(list: List<T>?): List<Map<String, Any?>>? {
        return list?.map { mapOf("value" to it) }
    }

    fun logIt() {
        info { "utils used" }
    }
}
