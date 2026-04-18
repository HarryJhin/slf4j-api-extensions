# slf4j-ktx

[![Kotlin](https://img.shields.io/badge/Kotlin-1.9.25-7F52FF.svg?logo=kotlin&logoColor=white)](https://kotlinlang.org)
[![SLF4J](https://img.shields.io/badge/SLF4J-1.7.36%2B-blue.svg)](https://www.slf4j.org/)
[![License](https://img.shields.io/badge/License-MIT-blue.svg)](LICENSE)

한국어: [README.ko.md](README.ko.md)

> **Release status**: `slf4j-ktx-core` ships as a stable Maven Central release. The optional compiler-plugin artifacts are **SNAPSHOT-only**, deferred until JetBrains provides first-class IDE integration for third-party compiler plugins. See [RELEASING.md](RELEASING.md).

**Zero-boilerplate SLF4J logging** for Kotlin. `import io.github.harryjhin.slf4j.ktx.*` and call `info { "msg" }` directly — no `LoggerFactory.getLogger(...)` declaration, no manual `isXxxEnabled` guard, correct logger name.

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

## The Solution

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

- No Logger declaration.
- Lazy — the message lambda only runs when the level is enabled.
- Correct logger name — `LoggerFactory.getLogger(OrderService::class.java)` under the hood; the logger is cached per class.
- IDE-friendly — every call resolves against real top-level extensions in `slf4j-ktx-core`, so no red squiggles.

## Quick Start

```kotlin
plugins {
    kotlin("jvm") version "1.9.25"
}

dependencies {
    implementation("io.github.harryjhin:slf4j-ktx-core:0.1.0")
    runtimeOnly("org.slf4j:slf4j-simple:2.0.13")    // or any SLF4J binding
}
```

Import the level extensions in each file that logs:

```kotlin
import io.github.harryjhin.slf4j.ktx.*
```

That's it. Core is the only dependency needed for day-to-day use. The `@Slf4j` annotation shipped with core is a flag consumed by the optional compiler plugin (see below) — ignore it otherwise.

## Patterns

### Object-level logging

```kotlin
object Registry {
    fun reload() {
        info { "reload start" }
    }
}
```

### Throwable overload

```kotlin
try { riskyOperation() }
catch (e: Exception) { error(e) { "operation failed" } }
```

### Marker-qualified logging

Marker overloads are `Logger` receiver extensions:

```kotlin
import io.github.harryjhin.slf4j.ktx.*
import org.slf4j.LoggerFactory
import org.slf4j.MarkerFactory

val AUDIT = MarkerFactory.getMarker("AUDIT")
val log = LoggerFactory.getLogger(Audit::class.java)

class Audit {
    fun record(userId: String) {
        log.info(AUDIT) { "user logged in: $userId" }
    }
}
```

### MDC scoping

```kotlin
import io.github.harryjhin.slf4j.ktx.*

withMDC("requestId" to requestId, "userId" to userId) {
    info { "processing" }   // MDC keys visible to the appender
}                           // MDC restored on exit (including on exception)
```

### `kotlin.error()` disambiguation

```kotlin
error("msg")          // → kotlin.error(Any) → throws IllegalStateException
error { "msg" }       // → T.error(() -> String) → logs at ERROR level
error(e) { "msg" }    // → T.error(Throwable, () -> String) → logs with throwable
```

Different argument shapes — no resolution ambiguity.

## Optional compiler plugin (preview)

`slf4j-ktx-core` is a complete runtime library on its own. An optional compiler plugin can be applied to turn each call site inside a designated class into a direct static-field read (`Companion.log.info(...)`) with no per-call cache lookup, and to auto-register additional trigger annotations.

Plugin artifacts are **SNAPSHOT-only** today — see the per-module documentation:

- [`slf4j-ktx-gradle-plugin/README.md`](slf4j-ktx-gradle-plugin/README.md) — main plugin, `@Slf4j` trigger + custom trigger DSL.
- [`slf4j-ktx-spring-gradle-plugin/README.md`](slf4j-ktx-spring-gradle-plugin/README.md) — Spring stereotype triggers (`@Component`, `@Service`, etc.).

When the plugin is applied, the call-site behavior changes as follows:

| Context | Without plugin | With plugin + triggered class |
|---|---|---|
| `info { }` inside a triggered (`@Slf4j` / Spring / custom) class | `LoggerFactory.getLogger(T::class.java)` cache lookup per call | direct `Companion.log.info(...)` static-field read |
| `info { }` inside a non-triggered class | cache lookup per call | same (cache lookup per call — plugin does not rewrite this case) |

## Design note: why a compiler plugin?

Because the runtime extensions ship with full default bodies, `slf4j-ktx-core` works standalone. The compiler plugin is deliberately positioned as an *optional optimization*, not a correctness requirement.

**The plugin exists for a larger long-term surface**, parked behind a limitation outside this project's control: IntelliJ IDEA does not currently give third-party compiler plugins a first-class IDE integration path. When that changes — a stable API for IDE-facing synthetic declarations, or inclusion of the plugin in Kotlin IDE plugin's bundled list — the plugin can extend to cover features the runtime-only path cannot, such as:

- IDE-visible synthetic `log: Logger` on `Companion`, so parameterized-message `log.info("{}", x)` and `log.isDebugEnabled` guards resolve without red squiggles.
- Diagnostic-level enforcement surfaced as live IDE warnings.
- Future work: automatic MDC propagation, structured-logging builders, marker-aware call-site rewrites.

Until then, the plugin stays minimal and ships as preview SNAPSHOT.

Background:
- [Kotlin Discussions — FIR plugin and IDE integration](https://discuss.kotlinlang.org/t/fir-plugin-and-ide-integration/29384)
- [KT-23696](https://youtrack.jetbrains.com/issue/KT-23696)
- [Kotlin External FIR Support (KEFS)](https://plugins.jetbrains.com/plugin/26480-kotlin-external-fir-support) — community bridge offering partial third-party IDE support today

## Modules

| Artifact | Status | Purpose |
|---|---|---|
| `io.github.harryjhin:slf4j-ktx-core` | **stable release** | `T.<level>` / Marker / MDC inline extensions. Also ships the `@Slf4j` annotation used as a flag by the optional plugin. |
| `io.github.harryjhin:slf4j-ktx-gradle-plugin` | SNAPSHOT-only (preview) | Main Gradle plugin — `slf4jKtx { }` DSL and IR rewriter |
| `io.github.harryjhin:slf4j-ktx-spring-gradle-plugin` | SNAPSHOT-only (preview) | Auto-applies main + registers Spring stereotype triggers |
| `io.github.harryjhin:slf4j-ktx-compiler-plugin-embeddable` | SNAPSHOT-only (preview) | Fat JAR of the compiler plugin itself — pulled in by the Gradle plugin, not consumed directly |

Internal compiler modules (`slf4j-ktx.common/.k1/.k2/.backend/.cli`) are not published.

## Compatibility

| Item | Requirement |
|---|---|
| Kotlin | 1.9.25 (this branch) |
| Java | 8+ (runtime target), 17+ (build) |
| SLF4J | 1.7.36+ (compatible with 2.0) |
| Gradle | 8.0+ (only needed when applying the optional plugin) |

## Releasing

Release process and branch/tag conventions: [RELEASING.md](RELEASING.md).

## For LLMs

- [`llms.txt`](llms.txt) — summary + links ([llmstxt.org](https://llmstxt.org/))
- [`llms-full.txt`](llms-full.txt) — complete API signatures and usage examples
- [`AGENTS.md`](AGENTS.md) — architecture map and build commands

## License

[MIT License](LICENSE)
