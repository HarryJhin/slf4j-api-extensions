# slf4j-ktx

[![Kotlin](https://img.shields.io/badge/Kotlin-1.9.25-7F52FF.svg?logo=kotlin&logoColor=white)](https://kotlinlang.org)
[![SLF4J](https://img.shields.io/badge/SLF4J-1.7.36%2B-blue.svg)](https://www.slf4j.org/)
[![License](https://img.shields.io/badge/License-MIT-blue.svg)](LICENSE)

한국어: [README.ko.md](README.ko.md)

> **Branch `1.9.25-release`** — Kotlin 1.9.25 paired release. Design migrated from `slf4j-extensions` (Companion-member synthesis) to a smaller, IDE-friendly surface: runtime `T.<level>{}` extensions + IR call-site rewriting on `@Slf4j`-annotated classes. See [CHANGELOG.md](CHANGELOG.md) for migration notes.
>
> **Release status**: only `slf4j-ktx-core` ships as a stable Maven Central release. The compiler-plugin artifacts (`slf4j-ktx-compiler-plugin-embeddable`, `slf4j-ktx-gradle-plugin`, `slf4j-ktx-spring-gradle-plugin`) are **SNAPSHOT-only** — stable release is deferred until JetBrains provides a first-class IDE integration story for third-party compiler plugins. See [RELEASING.md](RELEASING.md) for the reasoning.

**Zero-boilerplate SLF4J logging** for Kotlin. Annotate a class with `@Slf4j` and call `info { "msg" }` directly. The compiler plugin rewrites each call to a direct static-field access against a pre-generated `Logger`. Classes without `@Slf4j` still compile and work — they fall through to the runtime extension's body (`LoggerFactory.getLogger(T::class.java)`).

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

Logger boilerplate in every class. Manual `isXxxEnabled` to avoid unused string formatting.

## The Solution

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

- **No Logger declaration** — the plugin synthesizes a `log: Logger` property on `OrderService.Companion`.
- **Lazy** — the message lambda only runs when the level is enabled.
- **Correct Logger name** — `LoggerFactory.getLogger("com.example.OrderService")`, enclosing class FQN.
- **IDE-friendly** — `trace { … }` / `info { … }` resolve against real top-level extensions, so no red squiggles. Compile-time IR rewriting replaces the call with a direct static-field access when the enclosing class carries `@Slf4j`.

## Quick Start

```kotlin
plugins {
    kotlin("jvm") version "1.9.25"
    id("io.github.harryjhin.slf4j-ktx") version "1.9.25"
}

dependencies {
    implementation("io.github.harryjhin:slf4j-ktx-core:0.1.0")
    runtimeOnly("org.slf4j:slf4j-simple:2.0.13")    // or any SLF4J binding
}
```

In each file that logs, import the level extensions:

```kotlin
import io.github.harryjhin.slf4j.ktx.*
```

Annotate classes that need logging with `@Slf4j`.

## Patterns

### Object-level logging

```kotlin
@Slf4j
object Registry {
    fun reload() {
        info { "reload start" }   // members are injected onto the object itself
    }
}
```

### Throwable overload

```kotlin
try { riskyOperation() }
catch (e: Exception) { error(e) { "operation failed" } }
```

### Meta-annotation (1-hop)

Apply `@Slf4j` to your own annotation to turn it into a trigger:

```kotlin
@Slf4j
annotation class LoggedStereotype

@LoggedStereotype
class OrderService {
    fun process() { info { "…" } }
}
```

### Spring integration

```kotlin
plugins {
    id("io.github.harryjhin.slf4j-ktx.spring") version "1.9.25"
}

@Service
class OrderService {
    fun process() { info { "…" } }
}
```

The Spring integration plugin auto-applies the main plugin and adds six Spring stereotype FQNs as triggers: `@Component`, `@Controller`, `@Service`, `@Repository`, `@RestController`, `@ControllerAdvice`.

### Custom triggers

```kotlin
slf4jKtx {
    annotation("com.example.LoggedDomain")
}
```

Entries combine with `@Slf4j` and (if enabled) with Spring stereotypes.

### Marker-qualified logging

The Marker-qualified overloads are `Logger`-receiver extensions (unlike the plain `T.<level>` extensions, which expose lambda sugar):

```kotlin
import io.github.harryjhin.slf4j.ktx.*
import org.slf4j.MarkerFactory

val AUDIT = MarkerFactory.getMarker("AUDIT")

@Slf4j
class Audit {
    fun record(userId: String) {
        log.trace(AUDIT) { "user logged in: $userId" }   // `log` is the synthesized internal property
    }
}
```

Note: `log` is synthesized at IR time and has `internal` visibility. Your IDE will mark it as unresolved (this is a third-party compiler plugin limitation that does not affect compilation). Prefer the lambda-sugar level extensions where a Marker is not needed.

### MDC scoping

```kotlin
import io.github.harryjhin.slf4j.ktx.*

withMDC("requestId" to requestId, "userId" to userId) {
    info { "processing" }   // requestId / userId visible to appender
}                           // MDC restored on exit (including on exception)
```

### `kotlin.error()` disambiguation

```kotlin
error("msg")          // → kotlin.error(Any) → throws IllegalStateException
error { "msg" }       // → T.error(() -> String) → logs at ERROR level
error(e) { "msg" }    // → T.error(Throwable, () -> String) → logs with throwable
```

Different argument shapes — no resolution ambiguity.

## Where the extensions are visible

`T.<level>` extensions resolve against any receiver `T : Any`, so calling `info { }` works inside any class body. What changes is whether the plugin rewrites the call:

| Context | Call resolves? | Plugin rewrites? | Result |
|---|---|---|---|
| Member of `@Slf4j`-annotated class / object | yes | **yes** | direct `Companion.log` access — no reflection, no cache lookup |
| Member of non-triggered class | yes | no | runtime body — `LoggerFactory.getLogger(T::class.java)` cache lookup |
| Top-level function | no (no receiver) | — | compile error; add `@Slf4j` to an enclosing class |

## Modules (published)

| Artifact | Purpose |
|---|---|
| `io.github.harryjhin:slf4j-ktx-core` | `@Slf4j` annotation + `T.<level>` / Marker / MDC runtime extensions. Independent cadence (`coreVersion`). |
| `io.github.harryjhin:slf4j-ktx-compiler-plugin-embeddable` | Compiler plugin fat JAR. Resolved automatically by the Gradle plugin. |
| `io.github.harryjhin:slf4j-ktx-gradle-plugin` | Main Gradle plugin (`slf4jKtx { }` DSL). |
| `io.github.harryjhin:slf4j-ktx-spring-gradle-plugin` | Spring integration Gradle plugin. |

Internal compiler modules (`slf4j-ktx.common/.k1/.k2/.backend/.cli`) are not published.

## Use without the compiler plugin (optional)

You can use `slf4j-ktx-core` as a lightweight runtime-only logging library without
applying the Gradle plugin:

```kotlin
plugins {
    kotlin("jvm") version "1.9.25"
    // no id("io.github.harryjhin.slf4j-ktx")
}
dependencies {
    implementation("io.github.harryjhin:slf4j-ktx-core:0.1.0")
}
```

Call `info { }` / `error(e) { }` etc. as usual. Each call goes through
`LoggerFactory.getLogger(T::class.java)` with SLF4J's built-in cache — comparable
per-call cost to kotlin-logging. The `@Slf4j` annotation has no effect in this mode.

Enabling the plugin adds one thing on top: each call site at a triggered class
becomes a direct static-field read (`Companion.log.info(...)`) with no per-call
cache lookup.

## Design note: why a compiler plugin?

Because the runtime extensions ship with full default bodies, `slf4j-ktx-core` works
standalone. The compiler plugin is deliberately positioned as an *optional
optimization*, not a correctness requirement.

**The plugin exists for a larger long-term surface**, parked behind a limitation
outside this project's control: IntelliJ IDEA does not currently give third-party
compiler plugins a first-class IDE integration path. When that changes — a stable
API for IDE-facing synthetic declarations, or inclusion of the plugin in Kotlin
IDE plugin's bundled list — the plugin can extend to cover features the
runtime-only path cannot, such as:

- IDE-visible synthetic `log: Logger` on `Companion`, so parameterized-message
  `log.info("{}", x)` and `log.isDebugEnabled` guards resolve without red squiggles
- Diagnostic-level enforcement surfaced as live IDE warnings
- Future work: automatic MDC propagation, structured-logging builders, marker-aware
  call-site rewrites

Until then, the plugin stays minimal: Companion `log` synthesis + call-site IR
rewrite. Both are invisible to the IDE by design; neither blocks correct compilation.

Background:
- [Kotlin Discussions — FIR plugin and IDE integration](https://discuss.kotlinlang.org/t/fir-plugin-and-ide-integration/29384)
- [KT-23696](https://youtrack.jetbrains.com/issue/KT-23696)
- [Kotlin External FIR Support (KEFS)](https://plugins.jetbrains.com/plugin/26480-kotlin-external-fir-support) — community bridge offering partial third-party IDE support today

## Compatibility

| Item | Requirement |
|---|---|
| Kotlin | 1.9.25 (this branch) |
| Plugin version | = Kotlin version |
| `slf4j-ktx-core` | 0.1.0+ (independent cadence) |
| Java | 8+ (runtime target), 17+ (build) |
| SLF4J | 1.7.36+ (compatible with 2.0) |
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
