---
name: kotlin-compiler-plugin-dev
description: Kotlin 컴파일러 플러그인 코드 작성/수정/디버그 시 사용. extensions-k1, extensions-k2, extensions-backend, extensions-cli 모듈 작업 시 자동 트리거. 컴파일러 API, FIR 선언 생성, IR body 생성, 테스트 프레임워크 관련 작업에 반드시 사용. "compiler plugin", "FIR", "IR", "IrGenerationExtension", "FirDeclarationGenerationExtension", "컴파일러 플러그인" 키워드 포함 시에도 트리거.
---

# Kotlin Compiler Plugin 개발 가이드

빌드 Kotlin: 2.3.20. API를 추측하지 말 것 — 이 스킬에 내장된 패턴을 따를 것.

추가 소스 참조 필요 시: `/Users/jjh/Projects/kotlin/` (JetBrains/kotlin sparse checkout)

---

## FIR 선언 생성 — `FirDeclarationGenerationExtension`

### 프로퍼티 생성

`createMemberProperty` 시그니처 (from `compiler/fir/plugin-utils/.../PropertyBuildingContext.kt`):

```kotlin
fun FirExtension.createMemberProperty(
    owner: FirClassSymbol<*>,
    key: GeneratedDeclarationKey,
    name: Name,
    returnType: ConeKotlinType,
    isVal: Boolean = true,
    hasBackingField: Boolean = true,
    config: PropertyBuildingContext.() -> Unit = {}
): FirProperty
```

`PropertyBuildingContext`에서 사용 가능한 설정:
- `visibility = Visibilities.Private` (기본값 Public)
- `modality = Modality.FINAL` (기본값 FINAL)
- `extensionReceiverType(type)` — 확장 프로퍼티용
- `setter(visibility)` — var 프로퍼티의 setter visibility
- `withGeneratedDefaultInitializer()` — 기본 초기화자 생성 (backing field + not abstract일 때)

`generateProperties()` 반환값은 `listOf(property.symbol)`.

### 함수 생성

`createMemberFunction` 시그니처 (from `compiler/fir/plugin-utils/.../SimpleFunctionBuildingContext.kt`):

```kotlin
fun FirExtension.createMemberFunction(
    owner: FirClassSymbol<*>,
    key: GeneratedDeclarationKey,
    name: Name,
    returnType: ConeKotlinType,
    config: SimpleFunctionBuildingContext.() -> Unit = {}
): FirNamedFunction
```

`SimpleFunctionBuildingContext`에서 사용 가능한 설정:
- `visibility`, `modality` — DeclarationBuildingContext에서 상속
- `valueParameter(name: Name, type: ConeKotlinType)` — 값 파라미터 추가
- `typeParameter(name, variance)` — 타입 파라미터 추가
- `extensionReceiverType(type)` — 확장 함수용
- `withGeneratedDefaultBody()` — 기본 throw body 생성

`valueParameter` 상세 시그니처:
```kotlin
fun valueParameter(
    name: Name,
    type: ConeKotlinType,
    isCrossinline: Boolean = false,
    isNoinline: Boolean = false,
    isVararg: Boolean = false,
    hasDefaultValue: Boolean = false,
    key: GeneratedDeclarationKey = this.key
)
```

### 생성자 생성 (no-arg 참조)

```kotlin
val constructor = createConstructor(
    owner = ownerClass,
    key = PluginKey,
    isPrimary = false,
    generateDelegatedNoArgConstructorCall = false
)
return listOf(constructor.symbol)
```

### getCallableNamesForClass 패턴

```kotlin
override fun getCallableNamesForClass(
    classSymbol: FirClassSymbol<*>,
    context: MemberGenerationContext,
): Set<Name> {
    if (classSymbol !is FirRegularClassSymbol) return emptySet()
    // 필터링 로직...
    return setOf(Name.identifier("propertyName"), Name.identifier("functionName"))
    // 생성자: setOf(SpecialNames.INIT)
}
```

### ConeKotlinType 생성 방법

```kotlin
// 내장 타입
session.builtinTypes.unitType.coneType
session.builtinTypes.stringType.coneType
session.builtinTypes.booleanType.coneType

// ClassId로 타입 해석
val classSymbol = session.symbolProvider.getClassLikeSymbolByClassId(classId) as? FirRegularClassSymbol
val type = classSymbol?.defaultType()

// 제네릭 타입 (예: Function0<String>)
// ClassId.toLookupTag().constructClassType() 사용:
import org.jetbrains.kotlin.fir.types.constructClassType
import org.jetbrains.kotlin.fir.types.toLookupTag

val function0Type = ClassId(FqName("kotlin"), Name.identifier("Function0"))
    .toLookupTag()
    .constructClassType(typeArguments = arrayOf(stringType), isMarkedNullable = false)
```

---

## IR Body 생성 — `IrGenerationExtension`

### 패턴: IrVisitorVoid (JetBrains 템플릿 + no-arg)

```kotlin
class MyIrTransformer(private val context: IrPluginContext) : IrVisitorVoid() {

    override fun visitElement(element: IrElement) {
        when (element) {
            is IrDeclaration, is IrFile, is IrModuleFragment -> element.acceptChildrenVoid(this)
            else -> {}
        }
    }

    override fun visitSimpleFunction(declaration: IrSimpleFunction) {
        val origin = declaration.origin
        if (origin !is IrDeclarationOrigin.GeneratedByPlugin || origin.pluginKey != MyPluginKey) return
        require(declaration.body == null)
        declaration.body = generateBody(declaration)
    }

    override fun visitProperty(declaration: IrProperty) {
        val origin = declaration.origin
        if (origin !is IrDeclarationOrigin.GeneratedByPlugin || origin.pluginKey != MyPluginKey) return
        // backingField.initializer 설정
    }
}
```

### IR 노드 생성 (2.3.20)

IrImpl 직접 생성자는 internal. 대신:

```kotlin
// 상수
IrConstImpl.string(-1, -1, irBuiltIns.stringType, "value")

// 반환문
IrReturnImpl(-1, -1, irBuiltIns.nothingType, function.symbol, expression)

// 블록 body
irFactory.createBlockBody(-1, -1, listOf(statement1, statement2))

// 호출 — DeclarationIrBuilder 사용
val builder = DeclarationIrBuilder(context, function.symbol)
builder.irCall(functionSymbol)        // IrCall 생성
builder.irGet(valueParameter)         // IrGetValue 생성
builder.irString("value")             // IrConst<String> 생성
builder.irBlockBody { ... }           // IrBlockBody DSL
builder.irExprBody(expression)        // IrExpressionBody
builder.irIfThen(type, condition, thenPart)  // IrWhen
builder.irTemporary(expression)       // IrVariable + 블록에 추가
```

### 호출 인자 설정 (2.3.20)

```kotlin
// DEPRECATED (2.3.20에서 에러):
call.putValueArgument(0, expression)

// CORRECT:
val param = call.symbol.owner.parameters.first { it.kind == IrParameterKind.Regular }
call.arguments[param.indexInParameters] = expression
```

### 함수 파라미터 접근 (2.3.20)

```kotlin
// DEPRECATED:
function.valueParameters
function.dispatchReceiverParameter

// CORRECT:
val regularParams = function.parameters.filter { it.kind == IrParameterKind.Regular }
val dispatchReceiver = function.parameters.firstOrNull { it.kind == IrParameterKind.DispatchReceiver }
```

### 함수/클래스 탐색

```kotlin
// 외부 라이브러리 함수 탐색
context.referenceFunctions(CallableId(classId, Name.identifier("methodName")))
// 위가 deprecated이면:
context.finderForBuiltins().findFunctions(callableId)

// 클래스 참조
context.referenceClass(ClassId(FqName("org.slf4j"), Name.identifier("Logger")))
```

### no-arg의 IR body 생성 (K2 경로)

K2에서는 FIR에서 선언, IR에서 body만 채움:

```kotlin
// NoArgConstructorBodyIrGenerationExtension.kt
override fun visitConstructor(declaration: IrConstructor) {
    if (declaration.origin != IrDeclarationOrigin.GeneratedByPlugin(NoArgPluginKey)) return
    val klass = declaration.parent as? IrClass ?: return
    val superClass = klass.superTypes.mapNotNull(IrType::getClass).singleOrNull { ... }
    context.generateNoArgConstructorBody(declaration, klass, superConstructor, invokeInitializers)
}
```

---

## 테스트 — `kotlin-compiler-internal-test-framework`

JetBrains compiler-plugin-template (https://github.com/Kotlin/compiler-plugin-template) 구조 그대로 사용.

### 의존성 (build.gradle.kts)
```kotlin
testImplementation("org.jetbrains.kotlin:kotlin-compiler:2.3.20")
testImplementation("org.jetbrains.kotlin:kotlin-compiler-internal-test-framework:2.3.20")
testRuntimeOnly("org.jetbrains.kotlin:kotlin-reflect:2.3.20")
testRuntimeOnly("org.jetbrains.kotlin:kotlin-script-runtime:2.3.20")
testRuntimeOnly("org.jetbrains.kotlin:kotlin-annotations-jvm:2.3.20")
```

### 필수 opt-in (build.gradle.kts)
```kotlin
kotlin {
    compilerOptions {
        optIn.add("org.jetbrains.kotlin.compiler.plugin.ExperimentalCompilerApi")
        optIn.add("org.jetbrains.kotlin.ir.symbols.UnsafeDuringIrConstructionAPI")
    }
}
```

### Box 테스트 파일 (`testData/box/*.kt`)
```kotlin
// WITH_STDLIB

fun box(): String {
    // 테스트 로직
    return "OK"  // "OK" 반환 = 통과
}
```

### AbstractBoxTest
```kotlin
open class AbstractBoxTest : AbstractFirBlackBoxCodegenTestBase(FirParser.LightTree) {
    override fun createKotlinStandardLibrariesPathProvider() = EnvironmentBasedStandardLibrariesPathProvider
    override fun configure(builder: TestConfigurationBuilder) {
        super.configure(builder)
        with(builder) {
            configurePlugin()  // ExtensionRegistrarConfigurator 등록
        }
    }
}
```

### ExtensionRegistrarConfigurator
```kotlin
class ExtensionRegistrarConfigurator(testServices: TestServices) : EnvironmentConfigurator(testServices) {
    override fun CompilerPluginRegistrar.ExtensionStorage.registerCompilerExtensions(
        module: TestModule, configuration: CompilerConfiguration
    ) {
        with(MyCompilerPluginRegistrar()) { registerExtensions(configuration) }
    }
}
```
