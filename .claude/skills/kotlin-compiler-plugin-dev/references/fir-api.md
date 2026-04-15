# FIR Declaration Generation API

Source: `compiler/fir/plugin-utils/src/org/jetbrains/kotlin/fir/plugin/`

## createMemberProperty

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

PropertyBuildingContext 설정:
- `visibility = Visibilities.Private` (기본 Public)
- `modality = Modality.FINAL` (기본 FINAL)
- `extensionReceiverType(type)` — 확장 프로퍼티
- `setter(visibility)` — var setter visibility
- **`withGeneratedDefaultInitializer()`** — **필수** (backing field가 있는 비abstract 프로퍼티). 이것 없으면 FIR→IR 변환에서 프로퍼티가 누락됨. stub 초기화자(throw)를 생성하며, 실제 값은 IR에서 채움. (plugin-sandbox/DataFrameLikeTypeMembersGenerator에서 확인)

## createMemberFunction

```kotlin
fun FirExtension.createMemberFunction(
    owner: FirClassSymbol<*>,
    key: GeneratedDeclarationKey,
    name: Name,
    returnType: ConeKotlinType,
    config: SimpleFunctionBuildingContext.() -> Unit = {}
): FirNamedFunction
```

SimpleFunctionBuildingContext 설정:
- `visibility`, `modality` — DeclarationBuildingContext에서 상속
- `valueParameter(name, type)` — 값 파라미터 추가
- `typeParameter(name, variance)` — 타입 파라미터
- `extensionReceiverType(type)` — 확장 함수
- `withGeneratedDefaultBody()` — 기본 throw body

### valueParameter 시그니처

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

## createConstructor (no-arg 참조)

```kotlin
val constructor = createConstructor(
    owner = ownerClass,
    key = PluginKey,
    isPrimary = false,
    generateDelegatedNoArgConstructorCall = false
)
return listOf(constructor.symbol)
```

## getCallableNamesForClass 패턴

```kotlin
override fun getCallableNamesForClass(
    classSymbol: FirClassSymbol<*>,
    context: MemberGenerationContext,
): Set<Name> {
    if (classSymbol !is FirRegularClassSymbol) return emptySet()
    // 필터링...
    return setOf(Name.identifier("log"), Name.identifier("trace"))
    // 생성자: setOf(SpecialNames.INIT)
}
```

## ConeKotlinType 생성

```kotlin
// 내장 타입
session.builtinTypes.unitType.coneType
session.builtinTypes.stringType.coneType

// ClassId로 해석
val symbol = session.symbolProvider.getClassLikeSymbolByClassId(classId) as? FirRegularClassSymbol
val type = symbol?.defaultType()

// 제네릭 타입 (예: Function0<String>)
import org.jetbrains.kotlin.fir.types.constructClassType
import org.jetbrains.kotlin.fir.types.toLookupTag

val function0Type = ClassId(FqName("kotlin"), Name.identifier("Function0"))
    .toLookupTag()
    .constructClassType(typeArguments = arrayOf(stringType), isMarkedNullable = false)
```

## no-arg FIR 생성 참조 (FirNoArgConstructorGenerator)

- `getCallableNamesForClass` → 모든 클래스에 `SpecialNames.INIT` 반환
- `generateConstructors` → `shouldGenerateNoArgConstructor()` 체크 후 `createConstructor()` 호출
- 어노테이션 매칭: `session.noArgPredicateMatcher.isAnnotated(classSymbol)` 사용
- 기존 no-arg 생성자 존재 여부: `context.declaredScope.getDeclaredConstructors()` 사용
