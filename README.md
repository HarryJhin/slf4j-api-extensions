# slf4j-ktx

[![Kotlin](https://img.shields.io/badge/Kotlin-1.9.25-7F52FF.svg?logo=kotlin&logoColor=white)](https://kotlinlang.org)
[![SLF4J](https://img.shields.io/badge/SLF4J-1.7.36%2B-blue.svg)](https://www.slf4j.org/)
[![License](https://img.shields.io/badge/License-MIT-blue.svg)](LICENSE)

> **Branch `1.9.25-release`** — active rewrite. The old `slf4j-extensions` modules are being retired in favor of `slf4j-ktx` (Companion-centric synthesis, opt-in `@Slf4j`, Spring plugin, K2 support). Upgrade notes: [CHANGELOG.md](CHANGELOG.md).

**Zero-boilerplate SLF4J logging** for Kotlin. Annotate a class with `@Slf4j` and call `info { "msg" }` directly — the compiler plugin puts the Logger and level functions on the class's `Companion` for you.

## The Problem

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

Same Logger boilerplate in every class. Manual `isXxxEnabled` checks to avoid unused string concatenation.

## The Solution

```kotlin
import io.github.harryjhin.slf4j.ktx.Slf4j

@Slf4j
class OrderService {
    fun process(order: Order) {
        trace { "processing: ${order.id}" }
        info { "order completed" }
        error(exception) { "processing failed" }
    }
}
```

- **Companion-injected** — plugin never touches the user class body. `log`, `trace`, `debug`, `info`, `warn`, `error × 2` live on `OrderService.Companion` (auto-generated if absent).
- **Lazy** — `if (log.isXxxEnabled) log.xxx(message())`. Lambda is not evaluated when the level is disabled.
- **Correct Logger name** — `LoggerFactory.getLogger("com.example.OrderService")`, enclosing class FQN.
- **Compile-time only** — no runtime reflection, no class-graph scanning.

## Quick Start

```kotlin
plugins {
    kotlin("jvm") version "1.9.25"
    id("io.github.harryjhin.slf4j-ktx") version "1.9.25"   // plugin ver ≡ Kotlin ver
}

dependencies {
    implementation("io.github.harryjhin:slf4j-ktx-core:0.1.0")   // @Slf4j annotation + Marker/MDC utils
    runtimeOnly("org.slf4j:slf4j-simple:2.0.13")                 // or your preferred SLF4J binding
}
```

Annotate classes that need logging:

```kotlin
import io.github.harryjhin.slf4j.ktx.Slf4j

@Slf4j
class MyService {
    fun doWork() {
        info { "hello" }
    }
}
```

## Object-level logging

```kotlin
@Slf4j
object Registry {
    fun reload() {
        info { "reload start" }   // injected directly onto the object
    }
}
```

`object` declarations are already singletons — no Companion needed. The plugin injects members directly.

## Custom trigger annotations

```kotlin
slf4jKtx {
    annotation("com.example.LoggedDomain")
}

@LoggedDomain
class ReportGenerator {
    fun run() { info { "…" } }
}
```

## Meta-annotation (1-hop)

`@Slf4j` on your own annotation makes that annotation a trigger.

```kotlin
@Slf4j
annotation class LoggedStereotype

@LoggedStereotype
class OrderService {
    fun process() { info { "…" } }
}
```

## Spring integration

```kotlin
plugins {
    id("io.github.harryjhin.slf4j-ktx.spring") version "1.9.25"   // auto-applies main plugin
}

@Service
class OrderService {
    fun process() { info { "…" } }
}
```

The Spring plugin contributes six stereotype FQNs (`@Component`, `@Controller`, `@Service`, `@Repository`, `@RestController`, `@ControllerAdvice`) as triggers. You can still add your own via `slf4jKtx { annotation("…") }`.

## Throwable and MDC

```kotlin
try { riskyOperation() }
catch (e: Exception) { error(e) { "operation failed" } }

withMDC("requestId" to "abc-123") {
    info { "processing" }  // MDC contains requestId
}                          // MDC restored on exit
```

## Marker

Marker-qualified overloads stay in the runtime (not on the Companion — they have their own shape).

```kotlin
import io.github.harryjhin.slf4j.ktx.trace
import org.slf4j.MarkerFactory

val AUDIT = MarkerFactory.getMarker("AUDIT")

@Slf4j
class Audit {
    fun record() {
        log.trace(AUDIT) { "user logged in: $userId" }   // `log` is the internal Companion property
    }
}
```

## `kotlin.error()` disambiguation

```kotlin
error("msg")    // kotlin.error(Any) → throws IllegalStateException
error { "msg" } // Companion.error(() -> String) → logs at ERROR level
```

Different call syntax, no resolution ambiguity.

## Where members are visible

Companion members are accessible **unqualified from the enclosing class body**. Everything else needs its own `@Slf4j`.

| Context | `info { "…" }` unqualified? |
|---|---|
| Member function of the annotated class | Yes |
| Local function inside such a member | Yes |
| Nested class (must carry its own `@Slf4j`) | No |
| Subclass body (parent's Companion does not inherit) | No |
| Top-level function | No |

## Modules

| Artifact | Purpose |
|---|---|
| `io.github.harryjhin:slf4j-ktx-core` | `@Slf4j` annotation + Marker/MDC runtime extensions. Separate cadence (`coreVersion`). |
| `io.github.harryjhin:slf4j-ktx-compiler-plugin-embeddable` | Compiler plugin fat JAR. Resolved automatically by the Gradle plugin. |
| `io.github.harryjhin:slf4j-ktx-gradle-plugin` | Main Gradle plugin (`slf4jKtx { }` DSL). |
| `io.github.harryjhin:slf4j-ktx-spring-gradle-plugin` | Spring integration (auto-applies main + Spring stereotype FQNs). |

Internal compiler modules (`slf4j-ktx.common/.k1/.k2/.backend/.cli`) are not published.

## Compatibility

| Item | Requirement |
|---|---|
| Kotlin | 1.9.25 (this branch) |
| Plugin version | = Kotlin version |
| `slf4j-ktx-core` | Independent cadence; `0.1.0+` |
| Java | 8+ (runtime), 17+ (build) |
| SLF4J | 1.7.36+ |
| Gradle | 8.0+ |

## Releasing

Release process and branch/tag conventions: [RELEASING.md](RELEASING.md).

## For LLMs

Machine-readable docs:

- [`llms.txt`](llms.txt) — summary + links ([llmstxt.org](https://llmstxt.org/))
- [`llms-full.txt`](llms-full.txt) — complete API signatures and usage examples
- [`AGENTS.md`](AGENTS.md) — architecture map and build commands

## License

[MIT License](LICENSE)
