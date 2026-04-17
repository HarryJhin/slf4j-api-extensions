# slf4j-ktx

[![Kotlin](https://img.shields.io/badge/Kotlin-1.9.25-7F52FF.svg?logo=kotlin&logoColor=white)](https://kotlinlang.org)
[![SLF4J](https://img.shields.io/badge/SLF4J-1.7.36%2B-blue.svg)](https://www.slf4j.org/)
[![License](https://img.shields.io/badge/License-MIT-blue.svg)](LICENSE)

English: [README.md](README.md)

> **브랜치 `1.9.25-release`** — Kotlin 1.9.25 전용. 구 `slf4j-extensions`의 Companion 멤버 synthesis 방식에서 **런타임 `T.<level>{}` 확장함수 + IR call-site rewriting** 방식으로 전환. IDE에서 빨간 밑줄 없음. 이전 버전에서 이관 가이드는 [CHANGELOG.md](CHANGELOG.md) 참조.
>
> **릴리즈 현황**: `slf4j-ktx-core` 만 Maven Central에 정식 배포. 컴파일러 플러그인 3종(`slf4j-ktx-compiler-plugin-embeddable`, `slf4j-ktx-gradle-plugin`, `slf4j-ktx-spring-gradle-plugin`)은 **SNAPSHOT 전용** — JetBrains가 3rd-party 컴파일러 플러그인의 공식 IDE 통합 경로를 제공하기 전까지 정식 릴리즈를 연기. 자세한 이유는 [RELEASING.md](RELEASING.md) 참조.

Kotlin용 **Zero-boilerplate SLF4J 로깅** 플러그인. class에 `@Slf4j` 만 붙이면 `info { "메시지" }` 를 바로 호출할 수 있음. 컴파일러 플러그인이 각 호출부를 Companion에 미리 생성한 `Logger` static field 직접 접근으로 **IR 레벨에서 치환**. `@Slf4j` 없는 class에서도 정상 컴파일 + 동작 (런타임 확장함수 기본 body가 `LoggerFactory.getLogger(T::class.java)` 캐시 조회로 fallback).

## 문제

```kotlin
class OrderService {
    private val log = LoggerFactory.getLogger(OrderService::class.java)

    fun process(order: Order) {
        if (log.isTraceEnabled) log.trace("processing: ${order.id}")
        log.info("order completed")
        log.error("processing failed", exception)
    }
}
```

- Logger 선언이 모든 class에 반복됨
- 불필요한 문자열 생성을 피하려면 매번 `isXxxEnabled` 가드 수동 작성

## 해결

```kotlin
import io.github.harryjhin.slf4j.ktx.*

@Slf4j
class OrderService {
    fun process(order: Order) {
        trace { "processing: ${order.id}" }
        info { "order completed" }
        error(exception) { "processing failed" }
    }
}
```

- **Logger 선언 불필요** — 플러그인이 `OrderService.Companion`에 `log: Logger` 속성을 합성
- **지연 평가** — 로그 레벨이 꺼져 있으면 메시지 람다 자체를 실행하지 않음
- **Logger 이름 정확** — `LoggerFactory.getLogger("com.example.OrderService")`. 포함 class FQN 사용
- **IDE 친화** — `trace { … }` / `info { … }` 는 런타임 확장함수에 실제로 resolve되므로 빨간 밑줄 없음. 컴파일 시 IR rewriting이 `@Slf4j` 대상 class에 한해 Companion.log 직접 접근으로 치환

## 빠른 시작

```kotlin
plugins {
    kotlin("jvm") version "1.9.25"
    id("io.github.harryjhin.slf4j-ktx") version "1.9.25"
}

dependencies {
    implementation("io.github.harryjhin:slf4j-ktx-core:0.1.0")
    runtimeOnly("org.slf4j:slf4j-simple:2.0.13")    // 원하는 SLF4J 바인딩 사용
}
```

로깅을 사용하는 파일에 level 확장함수를 import:

```kotlin
import io.github.harryjhin.slf4j.ktx.*
```

로깅이 필요한 class에 `@Slf4j` 부착.

## 패턴

### object에서 사용

```kotlin
@Slf4j
object Registry {
    fun reload() {
        info { "reload 시작" }   // Companion 없이 object 자신에 주입됨
    }
}
```

### Throwable 오버로드

```kotlin
try { riskyOperation() }
catch (e: Exception) { error(e) { "작업 실패" } }
```

### 메타 어노테이션 (1-hop)

`@Slf4j` 를 사용자 정의 어노테이션에 부착하면 그 어노테이션 자체가 트리거가 됨:

```kotlin
@Slf4j
annotation class LoggedStereotype

@LoggedStereotype
class OrderService {
    fun process() { info { "…" } }
}
```

### Spring 통합

```kotlin
plugins {
    id("io.github.harryjhin.slf4j-ktx.spring") version "1.9.25"
}

@Service
class OrderService {
    fun process() { info { "…" } }
}
```

Spring 통합 플러그인은 메인 플러그인을 자동 apply하고 Spring stereotype 6개를 트리거로 등록: `@Component`, `@Controller`, `@Service`, `@Repository`, `@RestController`, `@ControllerAdvice`.

### 커스텀 트리거 어노테이션

```kotlin
slf4jKtx {
    annotation("com.example.LoggedDomain")
}
```

`@Slf4j`, (활성화 시) Spring stereotype과 함께 동작.

### Marker 기반 로깅

Marker 오버로드는 `Logger` receiver 확장함수 (평범한 `T.<level>` 확장과 다름):

```kotlin
import io.github.harryjhin.slf4j.ktx.*
import org.slf4j.MarkerFactory

val AUDIT = MarkerFactory.getMarker("AUDIT")

@Slf4j
class Audit {
    fun record(userId: String) {
        log.trace(AUDIT) { "사용자 로그인: $userId" }   // `log`는 합성된 internal 속성
    }
}
```

주의: `log` 는 IR 시점에 합성된 `internal` 속성이라 IDE가 unresolved로 표시할 수 있음(3rd-party 컴파일러 플러그인의 근본 제약이며 빌드/실행엔 영향 없음). Marker가 불필요하면 람다 슈가 확장(`info { }`)을 선호할 것.

### MDC 스코핑

```kotlin
import io.github.harryjhin.slf4j.ktx.*

withMDC("requestId" to requestId, "userId" to userId) {
    info { "요청 처리 중" }      // appender가 requestId / userId 참조 가능
}                               // 예외 발생 시에도 원복 보장
```

### `kotlin.error()` 충돌 없음

```kotlin
error("msg")          // → kotlin.error(Any) → IllegalStateException 던짐
error { "msg" }       // → T.error(() -> String) → ERROR 레벨로 로깅
error(e) { "msg" }    // → T.error(Throwable, () -> String) → throwable과 함께 로깅
```

인자 형태가 달라 overload resolution이 명확.

## 확장함수가 보이는 범위

`T.<level>` 확장함수는 receiver `T : Any` 에 대해 resolve되므로 **어떤 class 바디에서든 `info { }` 호출 성립**. 차이는 플러그인이 호출부를 치환하느냐 여부:

| 컨텍스트 | 호출 resolve? | 플러그인 IR 치환? | 실제 동작 |
|---|---|---|---|
| `@Slf4j` 붙은 class / object 내부 | 예 | **예** | Companion.log static field 직접 접근. reflection도 cache 조회도 없음 |
| 트리거 없는 class 내부 | 예 | 아니오 | 런타임 확장함수 기본 body 실행 → `LoggerFactory.getLogger(T::class.java)` 캐시 조회 |
| top-level 함수 | 아니오 (receiver 없음) | — | 컴파일 에러 — 포함하는 class에 `@Slf4j` 부착 |

## 배포 모듈

| 아티팩트 | 설명 |
|---|---|
| `io.github.harryjhin:slf4j-ktx-core` | `@Slf4j` 어노테이션 + `T.<level>` / Marker / MDC 런타임 확장. 독립 cadence (`coreVersion`) |
| `io.github.harryjhin:slf4j-ktx-compiler-plugin-embeddable` | 컴파일러 플러그인 fat JAR. Gradle 플러그인이 자동 resolve. 사용자가 직접 참조할 필요 없음 |
| `io.github.harryjhin:slf4j-ktx-gradle-plugin` | 메인 Gradle 플러그인 (`slf4jKtx { }` DSL) |
| `io.github.harryjhin:slf4j-ktx-spring-gradle-plugin` | Spring 통합 Gradle 플러그인 |

내부 컴파일러 모듈(`slf4j-ktx.common/.k1/.k2/.backend/.cli`)은 publish 대상 아님.

## 컴파일러 플러그인 없이 사용 (선택)

Gradle 플러그인을 적용하지 않고 `slf4j-ktx-core` 를 **런타임 전용 경량 로깅 라이브러리**로도 쓸 수 있음:

```kotlin
plugins {
    kotlin("jvm") version "1.9.25"
    // id("io.github.harryjhin.slf4j-ktx") 생략
}
dependencies {
    implementation("io.github.harryjhin:slf4j-ktx-core:0.1.0")
}
```

`info { }` / `error(e) { }` 등 호출 그대로 작동. 매 호출마다 `LoggerFactory.getLogger(T::class.java)` 가 SLF4J 내장 캐시를 조회하므로 kotlin-logging 수준의 호출당 비용. 이 모드에선 `@Slf4j` 어노테이션은 효과 없음.

플러그인을 활성화하면 추가 이득 하나: 트리거 class의 호출부는 IR에서 Companion static field 직접 접근(`Companion.log.info(...)`)으로 변환되어 호출당 cache lookup 제거.

## 설계 노트: 왜 컴파일러 플러그인인가

런타임 확장함수가 기본 body를 완비하기 때문에 `slf4j-ktx-core` 는 standalone으로도 동작. 컴파일러 플러그인은 의도적으로 **선택적 최적화** 포지션이지 정확성 요구사항 아님.

**플러그인이 존재하는 이유는 장기적 확장 여지** — 현재는 이 프로젝트가 통제할 수 없는 환경 제약 때문에 축소된 상태. IntelliJ IDEA가 3rd-party 컴파일러 플러그인에 first-class IDE 통합 경로(안정 API 또는 Kotlin IDE 플러그인 bundled 목록 편입)를 열면, 런타임만으로는 구현 불가한 기능으로 확장 예정:

- `Companion`의 synthetic `log: Logger` 를 IDE가 인식 — `log.info("{}", x)` 같은 파라미터형 메시지, `log.isDebugEnabled` 가드가 IDE 빨간 밑줄 없이 resolve
- 진단 수준 제약(`@Slf4j` 는 interface에 붙일 수 없음 등)을 IDE 경고로 실시간 제공
- 장래: 자동 MDC 전파, 구조화 로깅 빌더, marker-aware 호출부 재작성 등

현재까지 플러그인의 실 역할: Companion `log` synthesis + 호출부 IR 재작성. 두 기능 모두 IDE엔 보이지 않도록 설계(의도). 정상 컴파일을 방해하지 않음.

배경:
- [Kotlin Discussions — FIR plugin and IDE integration](https://discuss.kotlinlang.org/t/fir-plugin-and-ide-integration/29384)
- [KT-23696](https://youtrack.jetbrains.com/issue/KT-23696)
- [Kotlin External FIR Support (KEFS)](https://plugins.jetbrains.com/plugin/26480-kotlin-external-fir-support) — 커뮤니티가 만든 bridge, 부분적 IDE 지원 제공

## 호환성

| 항목 | 요구사항 |
|---|---|
| Kotlin | 1.9.25 (이 브랜치) |
| 플러그인 버전 | = Kotlin 버전 |
| `slf4j-ktx-core` | 0.1.0+ (독립 cadence) |
| Java | 8+ (런타임), 17+ (빌드) |
| SLF4J | 1.7.36+ (2.0 호환) |
| Gradle | 8.0+ |

## 릴리즈

릴리즈 프로세스 및 브랜치/태그 규약: [RELEASING.md](RELEASING.md).

## LLM용 문서

기계 판독 가능 문서:

- [`llms.txt`](llms.txt) — 요약 + 링크 ([llmstxt.org](https://llmstxt.org/))
- [`llms-full.txt`](llms-full.txt) — 전체 API 시그니처 + 사용 예시
- [`AGENTS.md`](AGENTS.md) — 아키텍처 지도 + 빌드 명령

## 라이선스

[MIT License](LICENSE)
