# IR Body Generation API (Kotlin 2.3.20)

## IrVisitorVoid 패턴

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

## IR 노드 생성

Impl 생성자는 internal — 빌더 사용:

```kotlin
// DeclarationIrBuilder — 모든 IR 노드 생성의 진입점
val builder = DeclarationIrBuilder(context, declaration.symbol)

builder.irCall(functionSymbol)                    // 함수 호출
builder.irGet(valueParameter)                     // 값 읽기
builder.irString("value")                         // 문자열 상수
builder.irBlockBody { ... }                       // 블록 body DSL
builder.irExprBody(expression)                    // 표현식 body
builder.irIfThen(type, condition, thenPart)       // if문
builder.irTemporary(expression)                   // 임시 변수 (블록에 자동 추가)

// 직접 생성 가능한 노드 (생성자가 public):
IrConstImpl.string(-1, -1, irBuiltIns.stringType, "value")
IrReturnImpl(-1, -1, irBuiltIns.nothingType, function.symbol, expression)
irFactory.createBlockBody(-1, -1, listOf(statement1, statement2))
```

## 2.3.20 API 변경 — `@DeprecatedForRemovalCompilerApi`

이 어노테이션이 붙은 API는 `@OptIn`이나 `@Suppress`로 억제 불가. 반드시 대체 API 사용.

### 호출 인자 설정 (power-assert 소스에서 확인)

```kotlin
// DEPRECATED:
call.putValueArgument(0, expression)

// CORRECT — 직접 인덱스 (power-assert: arguments[0], arguments[1]):
call.arguments[0] = expression
call.arguments[1] = anotherExpression
```

### 함수 파라미터 접근 (power-assert, noarg 소스에서 확인)

```kotlin
// DEPRECATED:
function.valueParameters

// CORRECT:
val regularParams = function.parameters.filter { it.kind == IrParameterKind.Regular }
val messageParam = function.parameters.last { it.kind == IrParameterKind.Regular }

// noarg는 Context 파라미터도 포함:
val valueParameters = parameters.filter { it.kind == IrParameterKind.Regular || it.kind == IrParameterKind.Context }
```

### 함수/클래스 탐색 (power-assert 소스에서 확인)

```kotlin
// 함수 탐색 — finderForBuiltins() 사용:
val finder = context.finderForBuiltins()
val functions = finder.findFunctions(CallableId(classId, Name.identifier("methodName")))
val clazz = finder.findClass(ClassId.topLevel(FqName("java.util.function.Supplier")))

// 클래스 참조:
context.referenceClass(ClassId(FqName("org.slf4j"), Name.identifier("Logger")))
```

> `referenceFunctions()`는 deprecated. `finderForBuiltins().findFunctions()` 사용.

## 이 프로젝트 — SLF4J Logger 주입 IR 패턴

```kotlin
// 1. LoggerFactory.getLogger(String) 호출 생성
val getLoggerSymbol = context.finderForBuiltins().findFunctions(
    CallableId(ClassId(FqName("org.slf4j"), Name.identifier("LoggerFactory")), Name.identifier("getLogger"))
).first { it.signature?.toString()?.contains("String") == true }

val builder = DeclarationIrBuilder(context, backingField.symbol)
backingField.initializer = builder.irExprBody(
    builder.irCall(getLoggerSymbol).also { call ->
        call.arguments[0] = builder.irString(className)
    }
)

// 2. if (log.isXxxEnabled) log.xxx(message.invoke()) body 생성
builder.irBlockBody {
    val logVal = irTemporary(irCall(logGetter).apply { dispatchReceiver = irGet(dispatchParam) })
    val msgExpr = irCall(invokeSymbol).apply { dispatchReceiver = irGet(messageParam) }
    val logCall = irCall(logMethodSymbol).also { call ->
        call.dispatchReceiver = irGet(logVal)
        call.arguments[0] = msgExpr
    }
    +irIfThen(context.irBuiltIns.unitType,
        irCall(isEnabledSymbol).apply { dispatchReceiver = irGet(logVal) },
        logCall)
}
```

## 현재 블로커

`FirSlf4jDeclarationGenerator.generateProperties()`에서 `createMemberProperty()`로 생성한 `log: Logger` 프로퍼티가 IR에 나타나지 않는다. IR의 `visitProperty`가 호출되지 않고 클래스 declarations에 IrProperty가 없음.

**조사 방향**: no-arg 플러그인은 `generateConstructors()`로 생성자를 만들고 IR에 나타남. 프로퍼티 생성 시 다른 점이 있는지 `/Users/jjh/Projects/kotlin/compiler/fir/plugin-utils/src/org/jetbrains/kotlin/fir/plugin/PropertyBuildingContext.kt`의 `build()` 메서드와 우리 `generateProperties()` 호출을 비교할 것.

## 필수 opt-in (build.gradle.kts)

```kotlin
kotlin {
    compilerOptions {
        optIn.add("org.jetbrains.kotlin.compiler.plugin.ExperimentalCompilerApi")
        optIn.add("org.jetbrains.kotlin.ir.symbols.UnsafeDuringIrConstructionAPI")
    }
}
```

## no-arg IR body 생성 참조 (K2 경로)

FIR에서 선언, IR에서 body만 채움:

```kotlin
override fun visitConstructor(declaration: IrConstructor) {
    if (declaration.origin != IrDeclarationOrigin.GeneratedByPlugin(NoArgPluginKey)) return
    val klass = declaration.parent as? IrClass ?: return
    val superClass = klass.superTypes.mapNotNull(IrType::getClass)
        .singleOrNull { it.kind == ClassKind.CLASS }
        ?: context.irBuiltIns.anyClass.owner
    val superConstructor = superClass.constructors
        .singleOrNull { it.isZeroParameterConstructor() }
    context.generateNoArgConstructorBody(declaration, klass, superConstructor, invokeInitializers)
}
```
