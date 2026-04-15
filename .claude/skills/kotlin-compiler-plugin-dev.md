---
name: kotlin-compiler-plugin-dev
description: Kotlin 컴파일러 플러그인 코드 작성/수정/디버그 시 사용. extensions-k1, extensions-k2, extensions-backend, extensions-cli 모듈 작업 시 자동 트리거. 컴파일러 API, FIR 선언 생성, IR body 생성, 테스트 프레임워크 관련 작업에 반드시 사용. "compiler plugin", "FIR", "IR", "IrGenerationExtension", "FirDeclarationGenerationExtension", "컴파일러 플러그인" 키워드 포함 시에도 트리거.
---

# Kotlin Compiler Plugin 개발 가이드

이 프로젝트는 Kotlin 2.3.20 컴파일러 플러그인이다. 컴파일러 내부 API는 버전마다 크게 바뀌고 문서가 부실하다. **추측하지 말고 소스를 읽어라.**

## 참조 소스 (반드시 먼저 읽을 것)

로컬 클론: `/Users/jjh/Projects/kotlin/`

| 작업 | 참조할 소스 | 핵심 파일 |
|------|-----------|----------|
| FIR 선언 생성 (프로퍼티, 함수) | `plugins/noarg/noarg.k2/` | `FirNoArgConstructorGenerator.kt` |
| FIR 상태 변환 | `plugins/allopen/allopen.k2/` | `FirAllOpenStatusTransformer.kt` |
| IR body 생성 | `plugins/noarg/noarg.backend/` | `noArgIrUtils.kt`, `NoArgConstructorBodyIrGenerationExtension.kt` |
| IR 변환 패턴 | `plugins/power-assert/power-assert-compiler/power-assert.backend/` | `PowerAssertCallTransformer.kt` |
| CompilerPluginRegistrar | `plugins/noarg/noarg.cli/` | `NoArgPlugin.kt` |
| FIR 헬퍼 (createMemberProperty 등) | `compiler/fir/plugin-utils/src/org/jetbrains/kotlin/fir/plugin/` | `PropertyBuildingContext.kt`, `SimpleFunctionBuildingContext.kt` |
| IR 빌더 API | `compiler/ir/backend.common/src/org/jetbrains/kotlin/backend/common/` | `lower/LowerUtils.kt` (DeclarationIrBuilder) |
| 테스트 프레임워크 | JetBrains compiler-plugin-template | https://github.com/Kotlin/compiler-plugin-template |

## API 탐색 절차

1. 소스에서 grep/Read로 해당 API를 찾는다
2. `@Deprecated` 어노테이션이 있으면 `replaceWith` 필드를 읽는다
3. 공식 플러그인(noarg, allopen, power-assert)에서 실제 사용 예를 찾는다
4. 그래도 모르면 "모르겠다"고 말한다

**금지**: javap로 바이트코드 역추적, 웹 크롤링으로 API 추측, `@Suppress`/`@OptIn`으로 deprecated API 억제 시도

## FIR 선언 생성 패턴

`FirDeclarationGenerationExtension` 구현 시:

```
1. getCallableNamesForClass() — 생성할 멤버 이름 반환
2. generateProperties() — createMemberProperty() 사용
3. generateFunctions() — createMemberFunction() 사용
```

**createMemberProperty/createMemberFunction 사용법을 모르면**:
→ `/Users/jjh/Projects/kotlin/compiler/fir/plugin-utils/src/org/jetbrains/kotlin/fir/plugin/` 소스를 읽어라.
→ noarg의 `FirNoArgConstructorGenerator`에서 실제 사용 패턴을 확인하라.

## IR Body 생성 패턴

JetBrains compiler-plugin-template의 `AbstractTransformerForGenerator` 패턴:

- `IrVisitorVoid` 상속
- `visitSimpleFunction`/`visitProperty`/`visitConstructor` 오버라이드
- `declaration.origin`이 `IrDeclarationOrigin.GeneratedByPlugin(PluginKey)`인지 체크
- body가 null인 선언만 처리
- IR 노드 직접 생성: `IrConstImpl`, `IrReturnImpl`, `irFactory.createBlockBody(-1, -1, statements)`
- 호출 생성: `DeclarationIrBuilder(context, symbol)` + `irCall()`, `irGet()`, `irBlockBody {}` 등 빌더 DSL

## 테스트

`kotlin-compiler-internal-test-framework` 사용 (JetBrains 공식).

테스트 설정을 모르면 → https://github.com/Kotlin/compiler-plugin-template 의 다음 파일을 읽어라:
- `compiler-plugin/build.gradle.kts` — 의존성, opt-in, 시스템 프로퍼티
- `compiler-plugin/test-fixtures/` — AbstractJvmBoxTest, ExtensionRegistrarConfigurator
- `compiler-plugin/testData/box/` — box 테스트 파일 형식

box 테스트 규칙: `fun box(): String` 반환값 `"OK"` = 통과.

## 현재 블로커

`FirSlf4jDeclarationGenerator.generateProperties()`로 생성한 `log` 프로퍼티가 IR에 나타나지 않음. noarg 플러그인의 `FirNoArgConstructorGenerator`와 비교하여 `createMemberProperty` 호출 방식 확인 필요.
