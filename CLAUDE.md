# slf4j-extensions

Kotlin compiler plugin that automatically injects SLF4J Logger and logging functions into classes.

## Commands

```bash
# Build all modules
./gradlew clean build

# Test runtime module only (38 tests)
./gradlew :slf4j-extensions-runtime:test

# Test compiler plugin — K1 + K2 box tests (20 tests)
./gradlew :slf4j-extensions-cli:test

# Compile specific module
./gradlew :slf4j-extensions-k2:compileKotlin
./gradlew :slf4j-extensions-backend:compileKotlin
./gradlew :slf4j-extensions-cli:compileKotlin

# Sample project (composite build)
cd sample && ../gradlew run
```

## Architecture

멀티모듈 Gradle 프로젝트 (Kotlin 2.3.20):

```
slf4j-extensions-common/         # 공유 상수 (PluginKey, ConfigKeys, PluginNames)
slf4j-extensions-k1/             # K1 프론트엔드 (SyntheticResolveExtension)
slf4j-extensions-k2/             # K2 FIR 프론트엔드 (FirDeclarationGenerationExtension)
slf4j-extensions-backend/        # IR 변환 (IrVisitorVoid 기반, K1/K2 공유)
slf4j-extensions-cli/            # 진입점 (CommandLineProcessor + CompilerPluginRegistrar)
slf4j-extensions-compiler/       # Fat JAR 패키징 (embedded 설정)
slf4j-extensions-runtime/        # 사용자 런타임 (Logger/Marker/MDC 확장)
slf4j-extensions-gradle-plugin/  # Gradle 플러그인 (KotlinCompilerPluginSupportPlugin)
sample/                          # 통합 테스트 (composite build)
```

**의존 방향**: cli → {k1, k2, backend} → common. runtime은 독립. gradle-plugin은 독립.

## Key Files

- `slf4j-extensions-k2/.../FirSlf4jDeclarationGenerator.kt` — FIR에서 log 프로퍼티 + 10개 inline 함수 선언
- `slf4j-extensions-k1/.../Slf4jSyntheticResolveExtension.kt` — K1 descriptor 생성 (프로퍼티 + 함수)
- `slf4j-extensions-backend/.../Slf4jIrTransformer.kt` — IR에서 함수 body 채우기 (K1/K2 공유)
- `slf4j-extensions-cli/.../Slf4jExtensionsCompilerPluginRegistrar.kt` — K1 + K2 + IR 확장 등록
- `slf4j-extensions-gradle-plugin/.../Slf4jExtensionsGradlePlugin.kt` — Gradle DSL + 컴파일러 플러그인 연결
- `slf4j-extensions-runtime/.../LoggerExtensions.kt` — inline fun Logger.trace/debug/info/warn/error

## Environment

- JDK 8+ (runtime target) — Adoptium toolchain 자동 다운로드 (foojay-resolver)
- JDK 17+ (build) — Kotlin 2.3.20 컴파일러 실행에 필요
- Gradle 8.8 (wrapper 포함)

## Compiler Plugin API (Kotlin 2.3.20)

필수 opt-in (build.gradle.kts): `ExperimentalCompilerApi`, `UnsafeDuringIrConstructionAPI`

- 심볼 탐색: `context.referenceClass(classId).owner.declarations` 순회 — deprecated `referenceFunctions` 미사용
- K1/K2 선언 식별: serialization의 `isFromPlugin(afterK2)` 패턴 — K2는 `GeneratedByPlugin(PluginKey)`, K1은 `descriptor.kind == SYNTHESIZED`
- 호출 인자: `call.arguments[param.indexInParameters] = expr`
- 함수 파라미터: `function.parameters.filter { it.kind == IrParameterKind.Regular }`
- IR 노드 생성: `DeclarationIrBuilder` + 빌더 DSL. 직접 Impl 생성자 호출 금지
- IR body 패턴: `IrVisitorVoid` + `visitSimpleFunction`/`visitProperty`
- 참고: `JetBrains/kotlin/plugins/kotlinx-serialization` (K1/K2 패턴)

## Testing

**Runtime tests**: `slf4j-test` (valfirst 3.0.x) + JUnit 5 — 38 tests
**Compiler tests**: `kotlin-compiler-internal-test-framework:2.3.20` — 20 box tests
- `testData/box/*.kt` — `fun box(): String` 반환값 `"OK"`이면 통과
- `K2BoxTestGenerated` → `AbstractK2BoxTest` (AbstractFirBlackBoxCodegenTestBase)
- `K1BoxTestGenerated` → `AbstractK1BoxTest` (AbstractIrBlackBoxCodegenTest)
- 같은 testData를 K1/K2 파이프라인으로 각각 실행

## Gotchas

### FIR (K2)
- `createMemberProperty`에 `withGeneratedDefaultInitializer()` 필수 — 없으면 FIR→IR 변환에서 프로퍼티 누락
- `createMemberFunction`에 `status { isInline = true }` — inline 함수 생성 시 필수

### K1 Descriptor
- `PropertyGetterDescriptorImpl`을 직접 생성하고 `initialize(returnType)` 호출 — `DescriptorFactory.createDefaultGetter`는 returnType 미설정
- `SimpleFunctionDescriptorImpl.create`에 `thisDescriptor.source` 사용 — `SourceElement.NO_SOURCE`는 psi2ir 크래시 유발
- K1 synthetic property의 backing field는 psi2ir에서 생성 안 됨 → IR에서 `createField`로 동적 생성
- K1 property getter body도 IR에서 명시 생성 필요 (K2는 FIR→IR이 자동 생성)

### IR
- Java stub `IrClassSymbol`과 Kotlin builtin `IrClassSymbol`은 다른 인스턴스 — `owner.name.asString()` 이름 비교 사용
- `referenceClass` + `declarations` 순회로 심볼 탐색 — `referenceFunctions`는 Java static 메서드 못 찾음, deprecated
- `@ObsoleteDescriptorBasedAPI`는 함수 레벨 `@OptIn`만 사용 (전역 opt-in 금지)

### 테스트
- 외부 라이브러리 타입 → `EnvironmentConfigurator.configureCompilerConfiguration`에서 `addJvmClasspathRoot` + `RuntimeClasspathProvider`
- box test에 `// FULL_JDK` 디렉티브 필요 — JDK 타입 사용 시
- `kotlin-compiler` (non-embeddable) vs `kotlin-compiler-embeddable` — 테스트에서는 non-embeddable, 메인 소스에서는 embeddable

## Specs & Plans

> **Note:** `docs/`가 `.gitignore`에 있어 git 추적 안 됨. v2에서 .gitignore 수정 필요.

- `docs/superpowers/specs/2026-04-15-slf4j-extensions-v2-design.md` — v2 설계 스펙
- `docs/superpowers/plans/2026-04-15-plan1-foundation-runtime.md` — Plan 1 (완료)
- `docs/superpowers/plans/2026-04-15-plan2-compiler-plugin.md` — Plan 2 (완료)
- `docs/superpowers/plans/2026-04-15-plan2-findings.md` — 2.3.20 API 발견사항 + 해결 기록

## Git

- `master` — v1.x (단일 모듈)
- `v2` — v2.0 개발 브랜치 (현재)
