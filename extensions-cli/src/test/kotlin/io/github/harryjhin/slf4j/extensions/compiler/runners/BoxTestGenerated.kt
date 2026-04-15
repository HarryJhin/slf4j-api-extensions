package io.github.harryjhin.slf4j.extensions.compiler.runners

import org.jetbrains.kotlin.test.TestMetadata
import org.junit.jupiter.api.Test

@TestMetadata("extensions-cli/testData/box")
class BoxTestGenerated : AbstractBoxTest() {

    @Test
    @TestMetadata("simpleTrace.kt")
    fun testSimpleTrace() {
        runTest("extensions-cli/testData/box/simpleTrace.kt")
    }

    @Test
    @TestMetadata("allLevels.kt")
    fun testAllLevels() {
        runTest("extensions-cli/testData/box/allLevels.kt")
    }

    @Test
    @TestMetadata("withThrowable.kt")
    fun testWithThrowable() {
        runTest("extensions-cli/testData/box/withThrowable.kt")
    }

    @Test
    @TestMetadata("kotlinErrorStillThrows.kt")
    fun testKotlinErrorStillThrows() {
        runTest("extensions-cli/testData/box/kotlinErrorStillThrows.kt")
    }

    @Test
    @TestMetadata("dataClass.kt")
    fun testDataClass() {
        runTest("extensions-cli/testData/box/dataClass.kt")
    }

    @Test
    @TestMetadata("objectClass.kt")
    fun testObjectClass() {
        runTest("extensions-cli/testData/box/objectClass.kt")
    }

    @Test
    @TestMetadata("skipInterface.kt")
    fun testSkipInterface() {
        runTest("extensions-cli/testData/box/skipInterface.kt")
    }

    @Test
    @TestMetadata("skipAnnotationClass.kt")
    fun testSkipAnnotationClass() {
        runTest("extensions-cli/testData/box/skipAnnotationClass.kt")
    }

    @Test
    @TestMetadata("existingLogProperty.kt")
    fun testExistingLogProperty() {
        runTest("extensions-cli/testData/box/existingLogProperty.kt")
    }

    @Test
    @TestMetadata("nestedClass.kt")
    fun testNestedClass() {
        runTest("extensions-cli/testData/box/nestedClass.kt")
    }
}
