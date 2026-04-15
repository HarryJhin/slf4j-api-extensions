---
name: kotlin-compiler-plugin-dev
description: Kotlin 컴파일러 플러그인 모듈(extensions-k1, k2, backend, cli) 작업 시 사용. FIR 선언 생성, IR body 생성, 테스트 작성에 필요한 API 패턴 제공. "compiler plugin", "FIR", "IR", "컴파일러 플러그인" 키워드에도 트리거.
---

# Kotlin Compiler Plugin 개발

빌드 Kotlin: 2.3.20. API를 추측하지 않는다 — 이 스킬의 레퍼런스를 따른다.

## 작업별 레퍼런스

| 작업 | 참조 |
|------|------|
| FIR 프로퍼티/함수/생성자 선언 | [references/fir-api.md](references/fir-api.md) |
| IR body 생성, 2.3.20 API 변경 | [references/ir-api.md](references/ir-api.md) |
| 테스트 프레임워크 설정 | [references/testing.md](references/testing.md) |

추가 소스 참조 필요 시: `/Users/jjh/Projects/kotlin/` (JetBrains/kotlin sparse checkout)

## 절차

### FIR 선언 생성 (`extensions-k2`)

1. `FirDeclarationGenerationExtension` 상속
2. `getCallableNamesForClass()`에서 생성할 이름 반환 (생성자는 `SpecialNames.INIT`)
3. `generateProperties()` — `createMemberProperty()` 사용
4. `generateFunctions()` — `createMemberFunction()` + `valueParameter()` 사용
5. 반환값은 항상 `listOf(declaration.symbol)`

타입 생성:
- 내장: `session.builtinTypes.unitType.coneType`
- ClassId: `session.symbolProvider.getClassLikeSymbolByClassId(id)?.defaultType()`
- 제네릭: `ClassId.toLookupTag().constructClassType(arrayOf(typeArg), isMarkedNullable = false)`

시그니처 상세와 설정 옵션은 [fir-api.md](references/fir-api.md) 참조.

### IR Body 생성 (`extensions-backend`)

1. `IrVisitorVoid` 상속 — `visitSimpleFunction`/`visitProperty` 오버라이드
2. `origin == GeneratedByPlugin(PluginKey)` 체크 후 body 설정
3. `DeclarationIrBuilder(context, symbol)` + 빌더 DSL로 IR 노드 생성
4. Impl 클래스 직접 생성 금지 — `irCall()`, `irGet()`, `irString()` 등 빌더 사용

2.3.20 변경사항과 패턴 상세는 [ir-api.md](references/ir-api.md) 참조.

### 테스트 (`extensions-cli`)

`kotlin-compiler-internal-test-framework:2.3.20` 사용. 설정은 [testing.md](references/testing.md) 참조.

## 금지사항

- javap로 API 추측
- `@Suppress("DEPRECATION_ERROR")`로 deprecated API 억제
- `IrCallImpl`, `IrClassReferenceImpl` 등 Impl 생성자 직접 호출
- 버전 다운그레이드로 API 호환 문제 회피
