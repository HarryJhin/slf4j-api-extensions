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

- `extensions-k2/src/main/kotlin/io/github/harryjhin/slf4j/extensions/compiler/k2/FirSlf4jDeclarationGenerator.kt` — FIR에서 log 프로퍼티 + 10개 함수 선언
- `extensions-backend/src/main/kotlin/io/github/harryjhin/slf4j/extensions/compiler/backend/Slf4jIrTransformer.kt` — IR에서 함수 body 채우기
- `extensions-cli/src/main/kotlin/io/github/harryjhin/slf4j/extensions/compiler/cli/Slf4jExtensionsCompilerPluginRegistrar.kt` — K2 FIR + IR 확장 등록
- `extensions-runtime/src/main/kotlin/io/github/harryjhin/slf4j/extensions/LoggerExtensions.kt` — inline fun Logger.trace/debug/info/warn/error

## Environment

- JDK 8+ (runtime target) — Adoptium toolchain 자동 다운로드 (foojay-resolver)
- JDK 17+ (build) — Kotlin 2.3.20 컴파일러 실행에 필요
- Gradle 8.8 (wrapper 포함)

## Kotlin 2.3.20 Compiler Plugin API

상세: `docs/superpowers/plans/2026-04-15-plan2-findings.md`

- 필수 opt-in: `ExperimentalCompilerApi`, `UnsafeDuringIrConstructionAPI` (build.gradle.kts에서 전역)
- `@DeprecatedForRemovalCompilerApi` 달린 API는 `@OptIn`/`@Suppress` 불가 — 대체 API 사용 필수
- IR 노드: `DeclarationIrBuilder` + 빌더 DSL 사용. 직접 Impl 생성자 호출 금지
- 함수 탐색: `referenceFunctions` deprecated -> `finderForBuiltins().findFunctions()` 사용
- 참고 템플릿: https://github.com/Kotlin/compiler-plugin-template

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

> **Note:** `docs/`가 `.gitignore`에 있어 git 추적 안 됨. v2에서 .gitignore 수정 필요.

- `docs/superpowers/specs/2026-04-15-slf4j-extensions-v2-design.md` — v2 설계 스펙
- `docs/superpowers/plans/2026-04-15-plan1-foundation-runtime.md` — Plan 1 (완료)
- `docs/superpowers/plans/2026-04-15-plan2-compiler-plugin.md` — Plan 2 (진행 중)
- `docs/superpowers/plans/2026-04-15-plan2-findings.md` — 2.3.20 API 발견사항

## Git

- `master` — v1.x (단일 모듈)
- `v2` — v2.0 개발 브랜치 (현재)
