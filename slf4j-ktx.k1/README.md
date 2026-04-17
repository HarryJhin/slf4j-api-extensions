# slf4j-ktx.k1

**K1 frontend** — descriptor 기반. `SyntheticResolveExtension`으로 `@Slf4j` 클래스에 Companion object를 자동 생성하고 그 위에 `log` + 5 level × 2 overload 함수 descriptor를 합성한다. K1 diagnostic checker와 version reader도 포함.

## 존재 이유

Kotlin 1.9.25의 기본 컴파일 경로는 여전히 K1이다. K1 frontend API는 `ClassDescriptor` / `PropertyDescriptor` / `SimpleFunctionDescriptor` 등 **descriptor 트리**를 다루며, K2 FIR의 `FirClassSymbol` / `FirRegularClass` 모델과 데이터 구조가 달라 **통합 불가능**. 별도 모듈로 분리하는 것이 강제된다.

Register-All 패턴(`plugins/kotlinx-serialization/kotlinx-serialization.cli/src/.../SerializationComponentRegistrar.kt:67-82`)에 따라 CLI가 K1/K2 확장을 조건 분기 없이 모두 등록하므로, 실제 활성 경로는 컴파일러가 `languageVersion`으로 선택한다.

## 주요 타입 (Step 5에서 구현)

- `Slf4jKtxResolveExtension` — `SyntheticResolveExtension` 구현. 4 override: `getSyntheticCompanionObjectNameIfNeeded`, `getSyntheticPropertiesNames`, `getSyntheticFunctionNames`, `generateSyntheticProperties`, `generateSyntheticMethods`
- `Slf4jKtxDescriptorResolver` — `PropertyDescriptorImpl` / `SimpleFunctionDescriptorImpl` 팩토리
- `Slf4jKtxClassFilter` — 트리거 어노테이션 매칭 + 1-hop 메타 어노테이션 탐지
- `Slf4jKtxDescriptorSerializerPlugin` — metadata 등록 훅 (빈 기본 구현; phantom Companion 검증 후 필요 시 확장)
- `Slf4jKtxVersionReader` — `slf4j-ktx-core` JAR 매니페스트에서 `Implementation-Version` / `Require-Kotlin-Version` 읽기
- `Slf4jKtxDeclarationChecker` — `CORE_MISSING` / `CORE_TOO_OLD` / `COMPILER_TOO_OLD` 진단
- `Slf4jKtxPluginComponentContainerContributor` — DeclarationChecker 등록
- `Slf4jKtxPluginErrors` / `Slf4jKtxPluginErrorsRendering` — 진단 팩토리 + 렌더러

## 의존

- `compileOnly`: `kotlin-compiler-embeddable`
- `implementation`: `:slf4j-ktx.common`

## 참조

- SPEC §4.1, §5.3, §6.3
- serialization 대응: `kotlinx-serialization.k1/src/.../extensions/SerializationResolveExtension.kt` (Companion synthesis 구조), `kotlinx-serialization.k1/src/.../resolve/KSerializerDescriptorResolver.kt` (`PropertyGetterDescriptorImpl.initialize(returnType)` 관용구), `kotlinx-serialization.k1/src/.../diagnostic/VersionReader.kt` (two-axis 버전 체크 읽기)
