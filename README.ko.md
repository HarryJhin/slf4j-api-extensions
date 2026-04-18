# slf4j-ktx

[![Kotlin](https://img.shields.io/badge/Kotlin-1.9.25-7F52FF.svg?logo=kotlin&logoColor=white)](https://kotlinlang.org)
[![SLF4J](https://img.shields.io/badge/SLF4J-1.7.36%2B-blue.svg)](https://www.slf4j.org/)
[![License](https://img.shields.io/badge/License-MIT-blue.svg)](LICENSE)

English: [README.md](README.md)

> **릴리즈 현황**: `slf4j-ktx-core` 만 Maven Central 정식 배포. 선택적 컴파일러 플러그인 artifact들은 **SNAPSHOT 전용** — JetBrains가 3rd-party 컴파일러 플러그인의 공식 IDE 통합 경로를 제공할 때까지 정식 릴리즈 연기. 상세는 [RELEASING.md](RELEASING.md).

Kotlin용 **Zero-boilerplate SLF4J 로깅** 라이브러리. `import io.github.harryjhin.slf4j.ktx.*` 후 `info { "메시지" }` 로 바로 호출. `LoggerFactory.getLogger(...)` 선언 불필요, `isXxxEnabled` 가드 불필요, logger 이름 자동.

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

## 해결

```kotlin
import io.github.harryjhin.slf4j.ktx.*

class OrderService {
    fun process(order: Order) {
        trace { "processing: ${order.id}" }
        info { "order completed" }
        error(exception) { "processing failed" }
    }
}
```

- Logger 선언 불필요
- 지연 평가 — 레벨이 꺼져 있으면 메시지 람다 자체를 실행 안 함
- Logger 이름 자동 — 내부적으로 `LoggerFactory.getLogger(OrderService::class.java)`, class별 cache
- IDE 친화 — 모든 호출이 `slf4j-ktx-core`의 실제 top-level 확장함수에 resolve. 빨간 밑줄 없음

## 빠른 시작

```kotlin
plugins {
    kotlin("jvm") version "1.9.25"
}

dependencies {
    implementation("io.github.harryjhin:slf4j-ktx-core:0.1.0")
    runtimeOnly("org.slf4j:slf4j-simple:2.0.13")    // 원하는 SLF4J 바인딩
}
```

로깅이 필요한 파일에 level 확장함수 import:

```kotlin
import io.github.harryjhin.slf4j.ktx.*
```

끝. core가 일상 사용에 필요한 유일한 의존. core에 포함된 `@Slf4j` 어노테이션은 선택적 컴파일러 플러그인의 트리거 **flag** 용도 — 플러그인 없이는 무시해도 됨.

## 패턴

### object 에서 사용

```kotlin
object Registry {
    fun reload() {
        info { "reload 시작" }
    }
}
```

### Throwable 오버로드

```kotlin
try { riskyOperation() }
catch (e: Exception) { error(e) { "작업 실패" } }
```

### Marker 기반 로깅

Marker 오버로드는 `Logger` receiver 확장함수:

```kotlin
import io.github.harryjhin.slf4j.ktx.*
import org.slf4j.LoggerFactory
import org.slf4j.MarkerFactory

val AUDIT = MarkerFactory.getMarker("AUDIT")
val log = LoggerFactory.getLogger(Audit::class.java)

class Audit {
    fun record(userId: String) {
        log.info(AUDIT) { "사용자 로그인: $userId" }
    }
}
```

### MDC 스코핑

```kotlin
import io.github.harryjhin.slf4j.ktx.*

withMDC("requestId" to requestId, "userId" to userId) {
    info { "요청 처리 중" }   // appender가 MDC 키 참조 가능
}                            // 예외 발생 시에도 원복 보장
```

### `kotlin.error()` 충돌 없음

```kotlin
error("msg")          // → kotlin.error(Any) → IllegalStateException 던짐
error { "msg" }       // → T.error(() -> String) → ERROR 로깅
error(e) { "msg" }    // → T.error(Throwable, () -> String) → throwable과 함께 로깅
```

인자 형태가 달라 overload resolution이 명확.

## 선택적 컴파일러 플러그인 (preview)

`slf4j-ktx-core` 는 단독으로 완전한 런타임 라이브러리. 선택적으로 컴파일러 플러그인을 적용하면 지정된 class 내부 호출부가 IR에서 직접 static field 접근(`Companion.log.info(...)`)으로 치환되어 호출당 cache lookup이 제거되고, 추가 트리거 annotation 등록이 가능.

플러그인 artifact는 **SNAPSHOT 전용**. 모듈별 문서 참조:

- [`slf4j-ktx-gradle-plugin/README.md`](slf4j-ktx-gradle-plugin/README.md) — 메인 플러그인, `@Slf4j` 트리거 + 커스텀 트리거 DSL
- [`slf4j-ktx-spring-gradle-plugin/README.md`](slf4j-ktx-spring-gradle-plugin/README.md) — Spring stereotype 트리거(`@Component`, `@Service` 등)

플러그인 적용 여부에 따른 호출부 동작:

| 컨텍스트 | 플러그인 없음 | 플러그인 + 트리거 class |
|---|---|---|
| 트리거 class(`@Slf4j` / Spring / custom) 내부 `info { }` | 호출당 `LoggerFactory.getLogger(T::class.java)` cache lookup | `Companion.log.info(...)` static field 직접 접근 |
| 트리거 없는 class 내부 `info { }` | 호출당 cache lookup | 동일 (플러그인이 이 경우는 치환 안 함) |

## 설계 노트: 왜 컴파일러 플러그인인가

런타임 확장함수가 기본 body를 완비하기 때문에 `slf4j-ktx-core` 는 standalone 으로도 동작. 컴파일러 플러그인은 의도적으로 **선택적 최적화** 포지션이지 정확성 요구사항 아님.

**플러그인이 존재하는 이유는 장기적 확장 여지** — 현재는 이 프로젝트가 통제할 수 없는 환경 제약 때문에 축소된 상태. IntelliJ IDEA가 3rd-party 컴파일러 플러그인에 first-class IDE 통합 경로를 열면, 런타임만으로는 구현 불가한 기능으로 확장 예정:

- `Companion` synthetic `log: Logger` 를 IDE가 인식 — `log.info("{}", x)`, `log.isDebugEnabled` 가 빨간 밑줄 없이 resolve
- 진단 수준 제약을 IDE 경고로 실시간 제공
- 장래: 자동 MDC 전파, 구조화 로깅 빌더, marker-aware 호출부 재작성 등

그 때까지 플러그인은 최소 범위로 preview SNAPSHOT 유지.

배경:
- [Kotlin Discussions — FIR plugin and IDE integration](https://discuss.kotlinlang.org/t/fir-plugin-and-ide-integration/29384)
- [KT-23696](https://youtrack.jetbrains.com/issue/KT-23696)
- [Kotlin External FIR Support (KEFS)](https://plugins.jetbrains.com/plugin/26480-kotlin-external-fir-support) — 커뮤니티가 만든 bridge, 부분적 IDE 지원 제공

## 배포 모듈

| 아티팩트 | 상태 | 설명 |
|---|---|---|
| `io.github.harryjhin:slf4j-ktx-core` | **정식 릴리즈** | `T.<level>` / Marker / MDC inline 확장. 선택적 플러그인이 트리거로 쓰는 `@Slf4j` 어노테이션도 이 모듈에 포함 |
| `io.github.harryjhin:slf4j-ktx-gradle-plugin` | SNAPSHOT (preview) | 메인 Gradle 플러그인 — `slf4jKtx { }` DSL + IR rewriter |
| `io.github.harryjhin:slf4j-ktx-spring-gradle-plugin` | SNAPSHOT (preview) | 메인 자동 apply + Spring stereotype 트리거 등록 |
| `io.github.harryjhin:slf4j-ktx-compiler-plugin-embeddable` | SNAPSHOT (preview) | 컴파일러 플러그인 fat JAR. Gradle 플러그인이 자동 resolve |

내부 컴파일러 모듈(`slf4j-ktx.common/.k1/.k2/.backend/.cli`)은 publish 대상 아님.

## 호환성

| 항목 | 요구사항 |
|---|---|
| Kotlin | 1.9.25 (이 브랜치) |
| Java | 8+ (런타임), 17+ (빌드) |
| SLF4J | 1.7.36+ (2.0 호환) |
| Gradle | 8.0+ (선택 플러그인 사용 시에만 필요) |

## 릴리즈

릴리즈 프로세스 및 브랜치/태그 규약: [RELEASING.md](RELEASING.md).

## LLM용 문서

- [`llms.txt`](llms.txt) — 요약 + 링크 ([llmstxt.org](https://llmstxt.org/))
- [`llms-full.txt`](llms-full.txt) — 전체 API 시그니처 + 사용 예시
- [`AGENTS.md`](AGENTS.md) — 아키텍처 지도 + 빌드 명령

## 라이선스

[MIT License](LICENSE)
