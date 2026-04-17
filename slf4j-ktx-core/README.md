# slf4j-ktx-core

**사용자 직접 사용 라이브러리** — `@Slf4j` 트리거 어노테이션과 SLF4J `Logger`에 대한 Marker-qualified inline 확장, `withMDC { }` 유틸리티를 제공한다. 컴파일러 플러그인이 생성하는 코드는 이 모듈을 **코드 레벨에서 참조하지 않는다** — 트리거 FQN만 매칭한다.

## 존재 이유

이 라이브러리는 **사용자 직접 호출 API**이지 플러그인이 뒤에서 참조하는 "런타임"이 아니다. serialization이 `kotlinx-serialization-core`로 불리는 이유와 동일 (`runtime` 네이밍은 plugin-종속 인상을 준다).

### 버전 정책 — plugin과 분리된 독립 cadence

- **plugin + compiler-internal 모듈**: Kotlin 컴파일러 버전과 1:1 pair (`1.9.25`)
- **`slf4j-ktx-core`**: 자체 semantic versioning (`0.1.0` 시작). 0.x 동안 API 자유롭게 진화, 완성되면 1.0.

### Manifest 두 속성

`META-INF/MANIFEST.MF`에 기록:
- `Implementation-Version` — 현재 core 버전
- `Require-Kotlin-Version` — 이 core 릴리즈가 요구하는 최소 Kotlin 컴파일러 버전

플러그인 측 `Slf4jKtxVersionReader` / `FirSlf4jKtxVersionReader`가 두 속성을 읽어 두 축 호환성 진단(`CORE_TOO_OLD` / `COMPILER_TOO_OLD`)을 낸다. (serialization `RuntimeVersions.kt` 패턴.)

## 주요 구성 (Step 4에서 구현)

- `Slf4j.kt` — `@Target(CLASS) @Retention(BINARY) annotation class Slf4j`
- `MarkerExtensions.kt` — `inline fun Logger.trace(marker, () -> String)` × 5 레벨 × 2 오버로드
- `MdcExtensions.kt` — `inline fun <R> withMDC(vararg entries: Pair<String, String?>, block: () -> R): R` — exception-safe MDC scoping

**없는 것**: plain `inline fun Logger.trace(() -> String)`. 그 역할은 플러그인이 Companion에 합성하는 멤버 함수가 담당한다. 외부 스코프에서 필요한 경우 해당 클래스에도 `@Slf4j`를 붙이는 것이 의도된 사용법 (Lombok `@Slf4j`와 동일).

## 의존

- `compileOnly`: `org.slf4j:slf4j-api:1.7.36`

## 참조

- SPEC §4.1, §5.8, §6.6
- serialization 대응: `kotlinx-serialization-core` (별도 repo: `github.com/Kotlin/kotlinx.serialization`). 우리는 단일 repo에 공존시키되 `coreVersion` property로 버전 분리
