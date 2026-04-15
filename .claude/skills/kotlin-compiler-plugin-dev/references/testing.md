# 테스트 프레임워크 — kotlin-compiler-internal-test-framework

JetBrains compiler-plugin-template 구조 그대로 사용.
Template: https://github.com/Kotlin/compiler-plugin-template

## 의존성 (build.gradle.kts)

```kotlin
dependencies {
    testImplementation("org.jetbrains.kotlin:kotlin-compiler:2.3.20")
    testImplementation("org.jetbrains.kotlin:kotlin-compiler-internal-test-framework:2.3.20")
    testImplementation(kotlin("test"))
    testImplementation(platform("org.junit:junit-bom:5.10.1"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testImplementation("org.junit.platform:junit-platform-commons")
    testImplementation("org.junit.platform:junit-platform-launcher")
    testImplementation("org.junit.platform:junit-platform-runner")
    testImplementation("org.junit.platform:junit-platform-suite-api")

    testRuntimeOnly("org.jetbrains.kotlin:kotlin-reflect:2.3.20")
    testRuntimeOnly("org.jetbrains.kotlin:kotlin-script-runtime:2.3.20")
    testRuntimeOnly("org.jetbrains.kotlin:kotlin-annotations-jvm:2.3.20")
}
```

## 테스트 태스크 설정

```kotlin
tasks.test {
    useJUnitPlatform()
    workingDir = rootDir

    doFirst {
        val cp = project.configurations.getByName("testRuntimeClasspath").files
        fun findJar(module: String): String =
            cp.find { it.name.matches(Regex("kotlin-${Regex.escape(module)}-\\d+\\..*\\.jar")) }
                ?.absolutePath ?: ""

        systemProperty("org.jetbrains.kotlin.test.kotlin-stdlib", findJar("stdlib"))
        systemProperty("org.jetbrains.kotlin.test.kotlin-stdlib-jdk8", findJar("stdlib-jdk8"))
        systemProperty("org.jetbrains.kotlin.test.kotlin-reflect", findJar("reflect"))
        systemProperty("org.jetbrains.kotlin.test.kotlin-test", findJar("test"))
        systemProperty("org.jetbrains.kotlin.test.kotlin-script-runtime", findJar("script-runtime"))
        systemProperty("org.jetbrains.kotlin.test.kotlin-annotations-jvm", findJar("annotations-jvm"))
    }

    systemProperty("idea.ignore.disabled.plugins", "true")
    systemProperty("idea.home.path", rootDir)
}
```

## Box 테스트 파일 (`testData/box/*.kt`)

```kotlin
// WITH_STDLIB

fun box(): String {
    val service = MyService()
    service.doWork()
    return "OK"  // "OK" 반환 = 통과
}

class MyService {
    fun doWork() {
        trace { "hello" }
    }
}
```

## AbstractBoxTest

```kotlin
open class AbstractBoxTest : AbstractFirBlackBoxCodegenTestBase(FirParser.LightTree) {
    override fun createKotlinStandardLibrariesPathProvider(): KotlinStandardLibrariesPathProvider {
        return EnvironmentBasedStandardLibrariesPathProvider
    }

    override fun configure(builder: TestConfigurationBuilder) {
        super.configure(builder)
        with(builder) {
            configurePlugin()
        }
    }
}
```

## ExtensionRegistrarConfigurator

```kotlin
fun TestConfigurationBuilder.configurePlugin() {
    useConfigurators(::ExtensionRegistrarConfigurator)
}

class ExtensionRegistrarConfigurator(testServices: TestServices) : EnvironmentConfigurator(testServices) {
    override fun CompilerPluginRegistrar.ExtensionStorage.registerCompilerExtensions(
        module: TestModule,
        configuration: CompilerConfiguration,
    ) {
        with(Slf4jExtensionsCompilerPluginRegistrar()) { registerExtensions(configuration) }
    }
}
```

## BoxTestGenerated (수동 작성 또는 GenerateTests로 자동 생성)

```kotlin
@TestMetadata("extensions-cli/testData/box")
class BoxTestGenerated : AbstractBoxTest() {
    @Test
    @TestMetadata("simpleTrace.kt")
    fun testSimpleTrace() {
        runTest("extensions-cli/testData/box/simpleTrace.kt")
    }
}
```
