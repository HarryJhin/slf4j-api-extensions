# slf4j-extensions (v1)

Kotlin 1.x 전용 컴파일러 플러그인. SLF4J Logger와 로깅 함수를 클래스에 자동 주입.

> **브랜치 전략** (kotlinx-serialization 패턴):
> - `v1` — **Kotlin 1.x 전용** (현재 브랜치, 1.9.25). K1 only.
> - `main` — Kotlin 2.x 전용 (K1 + K2).
> - 한 브랜치에서 두 메이저 버전을 동시 관리하지 않음.

## Commands

```bash
# 전체 빌드 + 모든 테스트
./gradlew clean build

# runtime 모듈만 (38 tests)
./gradlew :slf4j-extensions-runtime:test

# 컴파일러 플러그인 box tests (K1, 10 tests)
./gradlew :slf4j-extensions-cli:test

# 개별 모듈 컴파일
./gradlew :slf4j-extensions-k1:compileKotlin
./gradlew :slf4j-extensions-backend:compileKotlin
./gradlew :slf4j-extensions-cli:compileKotlin

# 샘플 (composite build)
cd sample && ../gradlew run
```

## Architecture

멀티모듈 Gradle 프로젝트 (Kotlin 1.9.25, K1 only):

```
slf4j-extensions-common/         # 공유 상수 (PluginKey, ConfigKeys, PluginNames)
slf4j-extensions-k1/             # K1 프론트엔드 (SyntheticResolveExtension)
slf4j-extensions-backend/        # IR 변환 (IrVisitorVoid 기반)
slf4j-extensions-cli/            # 진입점 (CommandLineProcessor + CompilerPluginRegistrar)
slf4j-extensions-compiler/       # Fat JAR 패키징 (embedded 설정)
slf4j-extensions-runtime/        # 사용자 런타임 (Logger/Marker/MDC 확장)
slf4j-extensions-gradle-plugin/  # Gradle 플러그인 (KotlinCompilerPluginSupportPlugin)
sample/                          # 통합 테스트 (composite build)
```

**의존 방향:** cli → {k1, backend} → common. runtime / gradle-plugin 독립.

## Key Files

- `slf4j-extensions-k1/.../Slf4jSyntheticResolveExtension.kt` — K1 descriptor 생성 (log 프로퍼티 + 5레벨×2오버로드)
- `slf4j-extensions-backend/.../Slf4jIrTransformer.kt` — IR에서 backing field + getter + 함수 body 생성
- `slf4j-extensions-cli/.../Slf4jExtensionsCompilerPluginRegistrar.kt` — K1 + IR 확장 등록 (`supportsK2 = false`)
- `slf4j-extensions-cli/.../Slf4jExtensionsCommandLineProcessor.kt` — CLI 옵션 파싱
- `slf4j-extensions-gradle-plugin/.../Slf4jExtensionsGradlePlugin.kt` — Gradle DSL + 컴파일러 플러그인 연결
- `slf4j-extensions-runtime/.../LoggerExtensions.kt` — inline fun Logger.trace/debug/info/warn/error

## Environment

- JDK 8+ (runtime target) — Adoptium toolchain 자동 다운로드 (foojay-resolver)
- JDK 17+ (build) — Kotlin 1.9.25 컴파일러 실행에 필요
- Gradle 8.8 (wrapper 포함)

## Compiler Plugin API (Kotlin 1.9.25)

필수 opt-in: `ExperimentalCompilerApi`

- K1 SyntheticResolveExtension: `getSyntheticPropertiesNames`, `generateSyntheticProperties`, `generateSyntheticMethods` 구현
- K1 선언 식별: `descriptor.kind == SYNTHESIZED` 또는 소유 클래스의 origin 조사 (kotlinx-serialization 패턴 참조)
- IR 노드 생성: `DeclarationIrBuilder` + 빌더 DSL. 직접 Impl 생성자 호출 금지
- IR body 패턴: `IrVisitorVoid` + `visitSimpleFunction` / `visitProperty`
- 참고: `JetBrains/kotlin/plugins/kotlinx-serialization` (K1 패턴)

## Testing

**Runtime tests**: `slf4j-test` (valfirst 3.0.x) + JUnit 5 — 38 tests
**Compiler box tests**: `kotlin-compiler-internal-test-framework:1.9.25` — 10 tests (K1 only)
- `testData/box/*.kt` — `fun box(): String` 반환값 `"OK"`이면 통과
- `K1BoxTestGenerated` → `AbstractK1BoxTest` (AbstractIrBlackBoxCodegenTest)

## Gotchas

### K1 Descriptor
- `PropertyGetterDescriptorImpl`을 직접 생성하고 `initialize(returnType)` 호출 — `DescriptorFactory.createDefaultGetter`는 returnType 미설정
- `SimpleFunctionDescriptorImpl.create`에 `thisDescriptor.source` 사용 — `SourceElement.NO_SOURCE`는 psi2ir 크래시 유발
- K1 synthetic property의 backing field는 psi2ir에서 생성 안 됨 → IR에서 `createField`로 동적 생성
- K1 property getter body도 IR에서 명시 생성 필요

### IR
- Java stub `IrClassSymbol`과 Kotlin builtin `IrClassSymbol`은 다른 인스턴스 — `owner.name.asString()` 이름 비교 사용
- `referenceClass` + `declarations` 순회로 심볼 탐색 — `referenceFunctions`는 Java static 메서드 못 찾음
- `@ObsoleteDescriptorBasedAPI`는 함수 레벨 `@OptIn`만 사용 (전역 opt-in 금지)

### 테스트
- 외부 라이브러리 타입 → `EnvironmentConfigurator.configureCompilerConfiguration`에서 `addJvmClasspathRoot` + `RuntimeClasspathProvider`
- box test에 `// FULL_JDK` 디렉티브 필요 — JDK 타입 사용 시
- `kotlin-compiler` (non-embeddable) vs `kotlin-compiler-embeddable` — 테스트에선 non-embeddable, 메인 소스에선 embeddable

## Publishing

- **Group:** `io.github.harryjhin`
- **Version:** `1.9.25-SNAPSHOT` (플러그인 버전 = Kotlin 버전)
- **배포 대상:** `slf4j-extensions-runtime`, `slf4j-extensions-compiler`, `slf4j-extensions-gradle-plugin`
- **저장소:** Maven Central (Sonatype OSSRH staging API)
- **서명:** non-SNAPSHOT 버전만 GPG 서명 필수

## Git

- `master` — 구버전 레거시 (단일 모듈 시절)
- `main` — Kotlin 2.x 전용 (K1 + K2)
- `v1` — **현재 브랜치** — Kotlin 1.5~1.9 지원 (K1 only)
