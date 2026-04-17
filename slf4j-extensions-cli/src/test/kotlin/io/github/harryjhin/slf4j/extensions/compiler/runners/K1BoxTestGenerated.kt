package io.github.harryjhin.slf4j.extensions.compiler.runners

import org.jetbrains.kotlin.test.TestMetadata
import org.junit.jupiter.api.Test

@TestMetadata("slf4j-extensions-cli/testData/box")
class K1BoxTestGenerated : AbstractK1BoxTest() {

    @Test
    @TestMetadata("simpleTrace.kt")
    fun testSimpleTrace() {
        runTest("slf4j-extensions-cli/testData/box/simpleTrace.kt")
    }

    @Test
    @TestMetadata("allLevels.kt")
    fun testAllLevels() {
        runTest("slf4j-extensions-cli/testData/box/allLevels.kt")
    }

    @Test
    @TestMetadata("withThrowable.kt")
    fun testWithThrowable() {
        runTest("slf4j-extensions-cli/testData/box/withThrowable.kt")
    }

    @Test
    @TestMetadata("kotlinErrorStillThrows.kt")
    fun testKotlinErrorStillThrows() {
        runTest("slf4j-extensions-cli/testData/box/kotlinErrorStillThrows.kt")
    }

    @Test
    @TestMetadata("dataClass.kt")
    fun testDataClass() {
        runTest("slf4j-extensions-cli/testData/box/dataClass.kt")
    }

    @Test
    @TestMetadata("objectClass.kt")
    fun testObjectClass() {
        runTest("slf4j-extensions-cli/testData/box/objectClass.kt")
    }

    @Test
    @TestMetadata("skipInterface.kt")
    fun testSkipInterface() {
        runTest("slf4j-extensions-cli/testData/box/skipInterface.kt")
    }

    @Test
    @TestMetadata("skipAnnotationClass.kt")
    fun testSkipAnnotationClass() {
        runTest("slf4j-extensions-cli/testData/box/skipAnnotationClass.kt")
    }

    @Test
    @TestMetadata("existingLogProperty.kt")
    fun testExistingLogProperty() {
        runTest("slf4j-extensions-cli/testData/box/existingLogProperty.kt")
    }

    @Test
    @TestMetadata("nestedClass.kt")
    fun testNestedClass() {
        runTest("slf4j-extensions-cli/testData/box/nestedClass.kt")
    }

    @Test
    @TestMetadata("objectWithGenericMethod.kt")
    fun testObjectWithGenericMethod() {
        runTest("slf4j-extensions-cli/testData/box/objectWithGenericMethod.kt")
    }

    @Test
    @TestMetadata("localAnonymousInsideGeneric.kt")
    fun testLocalAnonymousInsideGeneric() {
        runTest("slf4j-extensions-cli/testData/box/localAnonymousInsideGeneric.kt")
    }
}
