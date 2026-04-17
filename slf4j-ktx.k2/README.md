# slf4j-ktx.k2

**K2 FIR frontend** — `FirDeclarationGenerationExtension`으로 Companion object를 nested class로 생성하고 멤버를 합성한다. K2 전용 진단, version reader, session component가 여기 산다.

## 존재 이유

Kotlin 2.x의 기본 컴파일 경로. 1.9.25에서도 `languageVersion = "2.0"` opt-in으로 활성화 가능. K2 FIR은 **`FirSession` 기반의 예측 가능한 declaration-provider**와 `DeclarationPredicate` 기반 트리거 매칭을 지원하므로 K1 descriptor API와 구조적으로 분리해야 한다.

`FirAdditionalCheckersExtension` + `FirExtensionSessionComponent`를 통해 진단과 버전 캐싱도 여기서 담당.

## 주요 타입 (Step 6에서 구현)

- `FirSlf4jKtxExtensionRegistrar` — `FirExtensionRegistrar`. 4 확장 등록: `Slf4jKtxFirResolveExtension`, `FirSlf4jKtxMetadataSerializerPlugin`, `FirSlf4jKtxCheckersComponent`, `FirSlf4jKtxVersionReader`
- `Slf4jKtxFirResolveExtension` — `FirDeclarationGenerationExtension`. `getNestedClassifiersNames` + `generateNestedClassLikeDeclaration` (Companion 생성) + `getCallableNamesForClass` + `generateProperties` + `generateFunctions`
- `FirSlf4jKtxMetadataSerializerPlugin` — 빈 등록점 (phantom Companion 검증 후 필요 시 확장)
- `FirSlf4jKtxPredicates` — `DeclarationPredicate.create { annotated(*); metaAnnotated(*) }`
- `FirSlf4jKtxClassFilter` — 트리거 매칭 헬퍼
- `FirSlf4jKtxUtils` — type resolution helpers
- `checkers/FirSlf4jKtxCheckersComponent` — `FirAdditionalCheckersExtension`, config 생성자 주입
- `checkers/FirSlf4jKtxPluginClassChecker` — `FirClassChecker()` (1.9.25 no-arg), 3종 진단
- `checkers/FirSlf4jKtxErrors` + `KtDefaultErrorMessagesSlf4jKtx` — K2 진단 팩토리 + `RootDiagnosticRendererFactory.registerFactory(...)` init 등록
- `services/FirSlf4jKtxVersionReader` — `FirExtensionSessionComponent`, 세션당 캐시

## 의존

- `compileOnly`: `kotlin-compiler-embeddable`
- `implementation`: `:slf4j-ktx.common`

## 참조

- SPEC §4.1, §5.4, §6.4
- serialization 대응: `kotlinx-serialization.k2/src/.../fir/FirSerializationExtensionRegistrar.kt`, `kotlinx-serialization.k2/src/.../fir/SerializationFirResolveExtension.kt`, `kotlinx-serialization.k2/src/.../fir/FirSerializationPredicates.kt`, `kotlinx-serialization.k2/src/.../fir/checkers/FirSerializationPluginClassChecker.kt`, `kotlinx-serialization.k2/src/.../fir/services/FirVersionReader.kt`
