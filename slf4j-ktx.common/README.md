# slf4j-ktx.common

컴파일러 플러그인 모듈군의 **leaf** 모듈. 플러그인 키·설정 키·상수·Entity 이름·`Slf4jKtxConfig` data class를 담는다. Kotlin compiler API 외 어떤 내부 모듈도 의존하지 않는다.

## 존재 이유

K1 frontend, K2 FIR frontend, IR backend, CLI가 모두 **동일한 어노테이션 FQN·플러그인 키·트리거 설정**을 공유해야 한다. 이 계약을 한 곳에 고정하지 않으면 확장 간 drift가 발생해, 사용자가 `@Slf4j`를 달았는데 K1은 처리하고 K2는 무시하는 식의 비대칭이 생긴다.

`common`은 이 계약의 **유일 진실 원천(single source of truth)**이며, 런타임 의존이 없어 빌드 그래프의 루트를 차지한다.

## 주요 타입 (Step 3에서 구현)

- `Slf4jKtxPluginKey` — K2 FIR origin 식별자 (`GeneratedDeclarationKey` 서브타입)
- `Slf4jKtxConfigurationKeys` — 단일 `CompilerConfigurationKey<List<String>>` (`annotations`)
- `Slf4jKtxPluginNames` — `PLUGIN_ID`, `ANNOTATION_OPTION`, `LOG_PROPERTY_NAME`, `SLF4J_ANNOTATION_FQ_NAME`
- `Slf4jKtxEntityNames` — `LOGGER_CLASS_ID`, `LOG_LEVEL_NAMES`, `ALL_CALLABLE_NAMES`
- `Slf4jKtxConfig` — data class + `from(CompilerConfiguration)` factory

## 의존

- `compileOnly`: `org.jetbrains.kotlin:kotlin-compiler-embeddable:$kotlinVersion`
- 내부 모듈: 없음

## 참조

- SPEC §4.1 (모듈 구조), §5.2 (폴더 구조), §6.1 (코드 시그니처)
- serialization 대응: `kotlinx-serialization.common/` (공유 엔티티 이름·플러그인 키)
