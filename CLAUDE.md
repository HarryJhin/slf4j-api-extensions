# slf4j-ktx (Kotlin 1.9.25)

컴파일러 플러그인 + runtime library. Runtime의 `T.<level> { }` 확장함수가 기본 body(SLF4J LoggerFactory cache lookup)를 제공하므로 **core는 standalone으로도 동작**. Plugin은 `@Slf4j`(또는 Spring/custom trigger) 붙은 class의 호출부를 **IR에서 Companion.log static field 직접 접근으로 치환** — per-call cache lookup 제거. Plugin의 현 역할은 Companion `log` synthesis + call-site IR rewrite. 두 기능 모두 IDE엔 invisible (의도).

**릴리즈 정책**: `slf4j-ktx-core` 만 Maven Central 정식 배포 (독립 `coreVersion` semver). Plugin 3종은 SNAPSHOT 전용 — JetBrains 3rd-party 컴파일러 플러그인 IDE 통합 경로가 열릴 때까지 정식 릴리즈 연기. 상세: [RELEASING.md](RELEASING.md).

**플러그인의 장기 설계 의도**: 현재는 JetBrains 3rd-party IDE 지원 부재로 최적화 범위로 축소. 공식 stable API 또는 IDE plugin bundled 경로가 열리면 IDE-facing synthetic `log` 등 kotlinx-serialization 수준 surface로 확장 예정. 배경: [Kotlin Discussions FIR plugin & IDE](https://discuss.kotlinlang.org/t/fir-plugin-and-ide-integration/29384), [KT-23696](https://youtrack.jetbrains.com/issue/KT-23696), [KEFS](https://plugins.jetbrains.com/plugin/26480-kotlin-external-fir-support).

> **브랜치**: `1.9.25-release` — 현재 Kotlin 1.9.25 재작성 진행 중. 레거시 `slf4j-extensions-*` 8개 모듈은 아직 공존(후속 step에서 제거 예정). 진행 스냅샷: [docs/REWRITE-PROGRESS.md](docs/REWRITE-PROGRESS.md). 설계: [docs/REWRITE-SPEC.md](docs/REWRITE-SPEC.md).

## Commands

```bash
# 전체 빌드
./gradlew clean build

# 런타임 단위 테스트
./gradlew :slf4j-ktx-core:test

# 컴파일러 박스 테스트 (K1 13 + K2 13 = 26)
./gradlew :slf4j-ktx.cli:test

# 개별 모듈 컴파일
./gradlew :slf4j-ktx.common:compileKotlin
./gradlew :slf4j-ktx.k1:compileKotlin
./gradlew :slf4j-ktx.k2:compileKotlin
./gradlew :slf4j-ktx.backend:compileKotlin
./gradlew :slf4j-ktx.cli:jar
./gradlew :slf4j-ktx.embeddable:jar
./gradlew :slf4j-ktx-gradle-plugin:jar
./gradlew :slf4j-ktx-spring-gradle-plugin:jar
```

## Architecture

Kotlin 1.9.25 (K1 + K2 둘 다 등록, 컴파일러가 `languageVersion`으로 선택):

```
slf4j-ktx.common/       # 공유 상수 (PluginKey, Config, EntityNames, Versions)
slf4j-ktx.k1/           # K1 frontend — SyntheticResolveExtension + DescriptorSerializerPlugin + DeclarationChecker
slf4j-ktx.k2/           # K2 FIR frontend — FirDeclarationGenerationExtension + FirClassChecker + VersionReader session component
slf4j-ktx.backend/      # IR — Companion 멤버 body 생성 (log 초기화 + 레벨 함수)
slf4j-ktx.cli/          # Register-All CompilerPluginRegistrar + CommandLineProcessor (box 테스트 실행 sourceSet)
slf4j-ktx.embeddable/   # Fat JAR (embedded configuration + zipTree, shadow 미사용)
slf4j-ktx-core/         # 런타임 — @Slf4j + Marker/MDC (독립 coreVersion)
slf4j-ktx-gradle-plugin/          # 메인 Gradle plugin (slf4jKtx { } DSL)
slf4j-ktx-spring-gradle-plugin/   # Spring Plugin<Project> (메인 auto-apply + 6개 stereotype FQN push)

testData/box/                 # 박스 fixture 13개 (루트 레벨, serialization 패턴)
testFixtures/kotlin/          # 테스트 러너 4개 (AbstractBoxTest × 2, Configurator, ClasspathProvider)
tests-gen/                    # 수기 @TestMetadata 테스트 클래스 2개 (K1 + K2)
sample/                       # 통합 테스트 — 아직 레거시 슬레이브 참조(후속 step에서 slf4j-ktx로 전환)
```

**의존 방향:** cli → {k1, k2, backend} → common. runtime/gradle-plugin/spring-gradle-plugin 독립(core는 slf4j-api api 의존).

## Key Files

- `slf4j-ktx.k1/.../Slf4jKtxResolveExtension.kt` — K1 Companion 자동 생성 + log/레벨 함수 descriptor
- `slf4j-ktx.k1/.../Slf4jKtxDescriptorResolver.kt` — K1 PropertyDescriptor/SimpleFunctionDescriptor 빌드
- `slf4j-ktx.k2/.../Slf4jKtxFirResolveExtension.kt` — K2 Companion nested-class + callable + primary constructor
- `slf4j-ktx.backend/.../Slf4jKtxIrGenerator.kt` — IR body (`if (log.isXxxEnabled) log.xxx(message(), throwable?)`), Companion/object FQN 결정
- `slf4j-ktx.cli/.../Slf4jKtxComponentRegistrar.kt` — K1 + K2 + IR Register-All
- `slf4j-ktx-core/.../Slf4j.kt` — 트리거 어노테이션
- `slf4j-ktx-gradle-plugin/.../Slf4jKtxGradleSubplugin.kt` — `KotlinCompilerPluginSupportPlugin` + `slf4jKtx { annotation("…") }` DSL
- `slf4j-ktx-spring-gradle-plugin/.../Slf4jKtxSpringGradleSubplugin.kt` — `Plugin<Project>` + Spring FQN 6개 push

## Environment

- JDK 8+ (runtime target — Adoptium toolchain 자동 다운로드 via foojay-resolver)
- JDK 17+ (build — Kotlin 1.9.25 컴파일러 실행용)
- Gradle 8.8 (wrapper 포함)

## Compiler Plugin API (Kotlin 1.9.25)

필수 opt-in: `ExperimentalCompilerApi` (컴파일러-대면 모듈에 build.gradle.kts 단위로 적용됨)

### K1
- `SyntheticResolveExtension`: `getSyntheticCompanionObjectNameIfNeeded` / `getSyntheticPropertiesNames/FunctionNames` / `generateSyntheticProperties/Methods`
- **`thisDescriptor.companionObjectDescriptor` 쿼리 금지** — LockBasedStorageManager 재귀 유발 (serialization `SerializationResolveExtension.kt:66-70` 주석 참조). Kotlin 런타임이 Companion 없는 class에만 훅을 호출하므로 guard 불필요.
- 선언 식별: `descriptor.kind == CallableMemberDescriptor.Kind.SYNTHESIZED`
- 진단: `StorageComponentContainerContributor` + `DeclarationChecker` + `Errors.Initializer.initializeFactoryNamesAndDefaultErrorMessages`

### K2 (FIR)
- `FirDeclarationGenerationExtension`: `getNestedClassifiersNames` + `generateNestedClassLikeDeclaration` (`createCompanionObject(owner, pluginKey)`) → `getCallableNamesForClass` + `generateProperties/generateFunctions`
- **plugin-synthesized Companion에는 `generateConstructors` override 필수**. `createCompanionObject`만으로는 primary constructor가 emit되지 않아 `ObjectClassLowering`이 "Object should have a primary constructor: Companion" 실패. `getCallableNamesForClass`에서 `SpecialNames.INIT`을 plugin origin 시에만 추가하고 `generateConstructors`에서 `createDefaultPrivateConstructor(owner, Slf4jKtxPluginKey)` 반환 (serialization `SerializationFirResolveExtension.kt:99, 296-300` 패턴).
- Kotlin 1.9.25 부재 API: `FirMetadataSerializerPlugin`, `MppCheckerKind`, `registerDiagnosticContainers`, `FirClassLikeSymbol.sourceElement`. 각각 K1 `DescriptorSerializerPlugin`, `FirClassChecker()` no-arg, `RootDiagnosticRendererFactory.registerFactory` self-registration, stub 으로 대체.
- 진단 렌더러: `KtDiagnosticFactoryToRendererMap` 패키지는 `org.jetbrains.kotlin.diagnostics` (not `.rendering`), `CommonRenderers.STRING` (not `Renderers.STRING`)
- `isCompanion` import: `org.jetbrains.kotlin.fir.declarations.utils.isCompanion`

### IR
- `IrElementVisitorVoid` (deprecated 표기지만 1.9.25 기본) + `visitProperty` / `visitSimpleFunction`
- 선언 식별(`isFromPlugin(afterK2)`): K1이면 `SYNTHESIZED`, K2면 `IrDeclarationOrigin.GeneratedByPlugin(Slf4jKtxPluginKey)`
- `@ObsoleteDescriptorBasedAPI`는 함수 레벨 `@OptIn`만 (전역 opt-in 금지)
- Java stub `IrClassSymbol` vs Kotlin builtin `IrClassSymbol` 다름 — `owner.name.asString()` 이름 비교 사용. `referenceClass` + `declarations` 순회로 심볼 탐색(`referenceFunctions`는 Java static 못 찾음).
- K1 synthetic property의 backing field는 psi2ir에서 생성 안 됨 → IR에서 `createField`로 동적 생성 + getter body도 IR에서 명시 생성.

## Testing

**런타임 단위** (`slf4j-ktx-core:test`): slf4j-test (valfirst 3.0.x) + JUnit 5.

**박스 테스트** (`slf4j-ktx.cli:test`): `kotlin-compiler-internal-test-framework:1.9.25` — K1 13 + K2 13.
- `testData/box/*.kt` (루트) — `fun box(): String` 반환 `"OK"` 규칙.
- 러너 in `testFixtures/kotlin/.../runners/`: `AbstractSlf4jKtxK1BoxTest` (`AbstractIrBlackBoxCodegenTest`), `AbstractSlf4jKtxFirLightTreeBoxTest` (`AbstractFirLightTreeBlackBoxCodegenTest`).
- `generateTestGroupSuiteWithJUnit5` API는 Maven Central 미배포 → `tests-gen/`의 `K1BoxTestGenerated` / `FirLightTreeBoxTestGenerated`는 **수기 `@TestMetadata`**. 신규 fixture 추가 시 두 파일 모두 수동 갱신.
- **범위 밖(후속 작업)**: `customAnnotation`, `springService`(CLI option directive 필요), `runtimeMissing`, `runtimeTooOld`(diagnostics 테스트 인프라 필요).

### Fixture 작성 규칙
- `// WITH_STDLIB` + `// FULL_JDK` 디렉티브 필요.
- `import io.github.harryjhin.slf4j.ktx.Slf4j`
- Companion 멤버가 class 바디에서 unqualified 접근됨 (`trace { }`). 콜 사이트가 nested class나 subclass면 해당 클래스에도 `@Slf4j` 필요.

## Publishing

- **Group:** `io.github.harryjhin`
- **배포 대상:** `slf4j-ktx-core`, `slf4j-ktx-compiler-plugin-embeddable`, `slf4j-ktx-gradle-plugin`, `slf4j-ktx-spring-gradle-plugin`
- **버전 축:**
  - `version` (= `kotlinVersion`) — 플러그인 + embeddable + gradle plugins
  - `coreVersion` — `slf4j-ktx-core` 독립 cadence (시작 `0.1.0`)
  - `requireKotlin` — core JAR manifest에 stamp, 플러그인 VersionReader가 `COMPILER_TOO_OLD` 진단 시 사용
- **서명:** non-SNAPSHOT 버전만 GPG 필수 (`signingInMemoryKey` 기반)
- **Central Portal 전제:** namespace `io.github.harryjhin`에 "Enable SNAPSHOTs" 활성화됨
- **GitHub Actions secrets (`maven` environment):** `SONATYPE_USERNAME`, `SONATYPE_PASSWORD`, `GPG_SECRET_KEY`, `GPG_PASSPHRASE`

### Release 플로우 (SNAPSHOT → 검증 → Release)

3단계. 상세는 [RELEASING.md](RELEASING.md).

| 단계 | 트리거 | 워크플로우 | 결과 |
|------|--------|-----------|------|
| 1. SNAPSHOT publish | `{kotlinVersion}-release` 브랜치 push (`version=X-SNAPSHOT`) | `publish-snapshot.yml` | 모든 아티팩트 → Central Portal snapshot repo |
| 2. 다운스트림 검증 | 수동 | — | kovo-backend 등이 SNAPSHOT 의존성으로 빌드·테스트 |
| 3. Release publish | `v{kotlinVersion}` 태그 push (`version=X`) | `publish.yml` | Maven Central + GitHub Release |

**Safety:** `publish-snapshot.yml`는 version이 `-SNAPSHOT` 아니면 거부; `publish.yml`는 `-SNAPSHOT`이면 거부.

**Repository URL 분기** (`build.gradle.kts`): `version.endsWith("-SNAPSHOT")` 기준
- SNAPSHOT → `https://central.sonatype.com/repository/maven-snapshots/`
- Release → `https://ossrh-staging-api.central.sonatype.com/service/local/staging/deploy/maven2/`

## Git

- `master` — 구버전 레거시
- `main` — Kotlin 2.x 전용 (K1 + K2)
- `1.9.25-release` — **현재 브랜치** — Kotlin 1.9.25 재작성 진행 중 (`slf4j-extensions` → `slf4j-ktx`)
- `v1` — Kotlin 1.5~1.9 (슬레이브, 이전 구조 유지)
