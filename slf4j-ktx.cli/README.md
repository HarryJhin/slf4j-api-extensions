# slf4j-ktx.cli

컴파일러 entry point **aggregator**. `CompilerPluginRegistrar`로 K1/K2/IR 확장을 모두 등록하고 `CommandLineProcessor`로 CLI 옵션을 파싱한다.

## 존재 이유

`-Xplugin=...` 플래그로 JAR이 로드될 때 컴파일러가 찾는 **entry class는 서비스 디스크립터(`META-INF/services/`)로 광고되는 단일 `CompilerPluginRegistrar`**다. 모든 확장을 여기에 묶어야 한다.

**Register-All 패턴**: K1/K2 구분 없이 `SyntheticResolveExtension` / `FirExtensionRegistrarAdapter` / `IrGenerationExtension` / `StorageComponentContainerContributor` / `DescriptorSerializerPlugin`을 **조건 분기 없이 등록**한다. 컴파일러가 `languageVersion`으로 실제 활성 경로를 결정한다 (kotlinx-serialization `SerializationComponentRegistrar.kt:67-82`).

CLI 옵션은 serialization과 동일하게 **최소화**: `annotation` (multi-valued) 하나만 노출. 나머지는 전부 어노테이션 기반 opt-in으로 해결.

## 주요 타입 (Step 8에서 구현)

- `Slf4jKtxComponentRegistrar` — `CompilerPluginRegistrar`. `supportsK2 = true`. 5 확장 등록
- `Slf4jKtxPluginOptions` — `CommandLineProcessor`. `annotation` 옵션 1개, `allowMultipleOccurrences = true`
- `META-INF/services/org.jetbrains.kotlin.compiler.plugin.CompilerPluginRegistrar` + `…CommandLineProcessor` 서비스 파일

## 의존

- `compileOnly`: `kotlin-compiler-embeddable`
- `implementation`: `:slf4j-ktx.common`, `:slf4j-ktx.k1`, `:slf4j-ktx.k2`, `:slf4j-ktx.backend`

## 참조

- SPEC §4.1, §5.6, §6.2
- serialization 대응: `kotlinx-serialization.cli/src/.../extensions/SerializationComponentRegistrar.kt` — `SerializationPluginOptions` + `SerializationComponentRegistrar` 구조 동일
