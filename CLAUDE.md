# slf4j-extensions

Kotlin compiler plugin that automatically injects SLF4J Logger and logging functions into classes.

## Commands

```bash
# Build all modules
./gradlew clean build

# Test runtime module only (38 tests)
./gradlew :extensions-runtime:test

# Test compiler plugin (box tests)
./gradlew :extensions-cli:test

# Compile specific module
./gradlew :extensions-k2:compileKotlin
./gradlew :extensions-backend:compileKotlin
./gradlew :extensions-cli:compileKotlin
```

## Architecture

멀티모듈 Gradle 프로젝트 (Kotlin 2.3.20):

```
extensions-common/     # 공유 상수 (PluginKey, ConfigKeys, PluginNames)
extensions-k1/         # K1 프론트엔드 (SyntheticResolveExtension) — stub
extensions-k2/         # K2 FIR 프론트엔드 (FirDeclarationGenerationExtension)
extensions-backend/    # IR 변환 (IrVisitorVoid 기반)
extensions-cli/        # 진입점 (CommandLineProcessor + CompilerPluginRegistrar)
extensions-compiler/   # Fat JAR 패키징
extensions-runtime/    # 사용자 런타임 (Logger/Marker/MDC 확장)
```

**의존 방향**: cli → {k1, k2, backend} → common. runtime은 독립.

## Key Files

- `extensions-k2/.../FirSlf4jDeclarationGenerator.kt` — FIR에서 log 프로퍼티 + 10개 함수 선언
- `extensions-backend/.../Slf4jIrTransformer.kt` — IR에서 함수 body 채우기 (LoggerFactory.getLogger, isXxxEnabled 가드)
- `extensions-cli/.../Slf4jExtensionsCompilerPluginRegistrar.kt` — K2 FIR + IR 확장 등록
- `extensions-runtime/.../LoggerExtensions.kt` — inline fun Logger.trace/debug/info/warn/error

## Kotlin 2.3.20 Compiler Plugin API

### 필수 opt-in (build.gradle.kts)
```kotlin
kotlin {
    compilerOptions {
        optIn.add("org.jetbrains.kotlin.compiler.plugin.ExperimentalCompilerApi")
        optIn.add("org.jetbrains.kotlin.ir.symbols.UnsafeDuringIrConstructionAPI")
    }
}
```

### Deprecated API 대체 (2.3.20)
- `putValueArgument(i, expr)` -> `call.arguments[param.indexInParameters] = expr`
- `function.valueParameters` -> `function.parameters.filter { it.kind == IrParameterKind.Regular }`
- `IrCallImpl(...)` 직접 생성 금지 -> `DeclarationIrBuilder.irCall(symbol)` 사용
- `referenceFunctions(id)` -> `finderForBuiltins().findFunctions(id)` (외부 라이브러리용)
- `IrClass.functions` extension 금지 -> `declarations.filterIsInstance<IrSimpleFunction>()`

### IR 노드 생성 패턴 (JetBrains 템플릿)
- `IrVisitorVoid` + `visitSimpleFunction`/`visitProperty` — origin이 `GeneratedByPlugin(PluginKey)`인 것만 처리
- `irFactory.createBlockBody(-1, -1, statements)` — offset은 -1
- `IrConstImpl.string(-1, -1, type, value)` — 상수 생성
- `DeclarationIrBuilder(context, symbol)` — 빌더 DSL (`irCall`, `irGet`, `irBlockBody` 등)

## Testing

**Runtime tests**: `slf4j-test` (valfirst 3.0.x) + JUnit 5
**Compiler tests**: `kotlin-compiler-internal-test-framework:2.3.20` (JetBrains 공식)
- `testData/box/*.kt` — `fun box(): String` 반환값 `"OK"`이면 통과
- `AbstractBoxTest` -> `AbstractFirBlackBoxCodegenTestBase(FirParser.LightTree)`

## Gotchas

- `@DeprecatedForRemovalCompilerApi` — `@OptIn`이나 `@Suppress`로 억제 불가. 반드시 대체 API 사용
- `IrSymbol.owner` — `@UnsafeDuringIrConstructionAPI` opt-in 필요 (build.gradle.kts에서 전역 설정)
- `kotlin-compiler` (non-embeddable) vs `kotlin-compiler-embeddable` — 테스트에서는 non-embeddable, 메인 소스에서는 embeddable 사용. 같은 classpath에 공존 불가
- `createMemberProperty`로 생성한 FIR 프로퍼티가 IR에 나타나지 않는 문제 조사 중 (현재 블로커)

## Specs & Plans

- `docs/superpowers/specs/2026-04-15-slf4j-extensions-v2-design.md` — v2 설계 스펙
- `docs/superpowers/plans/2026-04-15-plan1-foundation-runtime.md` — Plan 1 (완료)
- `docs/superpowers/plans/2026-04-15-plan2-compiler-plugin.md` — Plan 2 (진행 중)
- `docs/superpowers/plans/2026-04-15-plan2-findings.md` — 2.3.20 API 발견사항

## Git

- `master` — v1.x (단일 모듈)
- `v2` — v2.0 개발 브랜치 (현재)
