package io.github.harryjhin.slf4j.ktx.runners

import org.jetbrains.kotlin.test.TestMetadata
import org.junit.jupiter.api.Test

/**
 * K1 frontend × IR backend box tests over `testData/box`. Hand-written because
 * `generateTestGroupSuiteWithJUnit5` is not distributed in the Maven `kotlin-compiler-
 * internal-test-framework:1.9.25` artifact. Keep alphabetical order; add a new `@Test`
 * for each new fixture under testData/box.
 */
@TestMetadata("testData/box")
class K1BoxTestGenerated : AbstractSlf4jKtxK1BoxTest() {

    @Test @TestMetadata("allLevels.kt")
    fun testAllLevels() = runTest("testData/box/allLevels.kt")

    @Test @TestMetadata("dataClass.kt")
    fun testDataClass() = runTest("testData/box/dataClass.kt")

    @Test @TestMetadata("existingCompanion.kt")
    fun testExistingCompanion() = runTest("testData/box/existingCompanion.kt")

    @Test @TestMetadata("existingLevelFunction.kt")
    fun testExistingLevelFunction() = runTest("testData/box/existingLevelFunction.kt")

    @Test @TestMetadata("existingLogProperty.kt")
    fun testExistingLogProperty() = runTest("testData/box/existingLogProperty.kt")

    @Test @TestMetadata("kotlinErrorStillThrows.kt")
    fun testKotlinErrorStillThrows() = runTest("testData/box/kotlinErrorStillThrows.kt")

    @Test @TestMetadata("metaAnnotation.kt")
    fun testMetaAnnotation() = runTest("testData/box/metaAnnotation.kt")

    @Test @TestMetadata("nestedClass.kt")
    fun testNestedClass() = runTest("testData/box/nestedClass.kt")

    @Test @TestMetadata("objectClass.kt")
    fun testObjectClass() = runTest("testData/box/objectClass.kt")

    @Test @TestMetadata("simpleInjection.kt")
    fun testSimpleInjection() = runTest("testData/box/simpleInjection.kt")

    @Test @TestMetadata("skipAnnotationClass.kt")
    fun testSkipAnnotationClass() = runTest("testData/box/skipAnnotationClass.kt")

    @Test @TestMetadata("skipInterface.kt")
    fun testSkipInterface() = runTest("testData/box/skipInterface.kt")

    @Test @TestMetadata("withThrowable.kt")
    fun testWithThrowable() = runTest("testData/box/withThrowable.kt")
}
