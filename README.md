# slf4j-extensions

[![Kotlin](https://img.shields.io/badge/Kotlin-1.5--1.9-7F52FF.svg?logo=kotlin&logoColor=white)](https://kotlinlang.org)
[![SLF4J](https://img.shields.io/badge/SLF4J-1.7.36%2B-blue.svg)](https://www.slf4j.org/)
[![License](https://img.shields.io/badge/License-MIT-blue.svg)](LICENSE)

> **Branch `v1`** — Kotlin 1.x 전용 (K1 compiler). Kotlin 2.x는 `main` 브랜치 사용.

**Zero-boilerplate SLF4J logging** for Kotlin — a compiler plugin that auto-injects Logger and inline logging functions into your classes.

## The Problem

Every class that logs needs the same boilerplate:

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

Logger declaration repeated in every class. Manual `isXxxEnabled` checks to avoid string concatenation overhead.

## The Solution

```kotlin
class OrderService {
    fun process(order: Order) {
        trace { "processing: ${order.id}" }
        info { "order completed" }
        error(exception) { "processing failed" }
    }
}
```

- **Zero boilerplate** — no Logger declaration needed
- **Lazy evaluation** — lambda not evaluated when level is disabled (inline functions)
- **Correct Logger name** — always matches the class (`com.example.OrderService`)
- **No runtime magic** — pure compile-time code generation

## Quick Start

```kotlin
// build.gradle.kts
plugins {
    kotlin("jvm") version "1.9.25"
    id("io.github.harryjhin.slf4j-extensions") version "1.9.25"  // Kotlin 버전과 동일
}
```

That's it. Write `trace { }`, `debug { }`, `info { }`, `warn { }`, `error { }` in any class.

## Configuration

```kotlin
slf4jExtensions {
    propertyName = "log"     // Logger property name (default: "log")
    allClasses = true        // Apply to all classes (default: true)

    // Or filter by annotation / package:
    annotation("com.example.Logged")
    packages("com.example.service")
}
```

## Features

### Lazy Message Evaluation

```kotlin
// Lambda is NOT called when trace is disabled
trace { "expensive computation: ${heavyToString()}" }
```

### Throwable Support

```kotlin
try {
    riskyOperation()
} catch (e: Exception) {
    error(e) { "operation failed" }
}
```

### Marker Support (via runtime extensions)

```kotlin
val AUDIT = MarkerFactory.getMarker("AUDIT")
log.info(AUDIT) { "user logged in: $userId" }
```

### MDC Scoping

```kotlin
withMDC("requestId" to "abc-123") {
    info { "processing" }  // MDC contains requestId
}
// MDC automatically restored
```

### `kotlin.error()` Compatibility

```kotlin
error("msg")    // -> kotlin.error() -> throws IllegalStateException
error { "msg" } // -> logging function -> logs at ERROR level
```

Different syntax (`()` vs `{}`), no ambiguity.

## Modules

| Module | Description |
|--------|-------------|
| `slf4j-extensions-runtime` | Inline Logger/Marker/MDC extension functions |
| `slf4j-extensions-compiler` | Compiler plugin (fat JAR) |
| `slf4j-extensions-gradle-plugin` | Gradle plugin for one-liner setup |

## Compatibility

플러그인 버전을 **사용 중인 Kotlin 버전과 동일하게** 지정합니다.

```kotlin
kotlin("jvm") version "1.9.25"
id("io.github.harryjhin.slf4j-extensions") version "1.9.25"  // 동일
```

| Item | Requirement |
|------|-------------|
| Plugin version | = Kotlin version |
| Kotlin | 1.5 – 1.9 (이 브랜치) |
| Java | 8+ |
| SLF4J | 1.7.36+ |
| Gradle | 8.0+ |

## Releasing

릴리즈 프로세스·브랜치/태그 규약은 [RELEASING.md](RELEASING.md) 참조.

## For LLMs

This project provides machine-readable documentation:

- [`llms.txt`](llms.txt) — Summary + links ([llmstxt.org](https://llmstxt.org/) standard)
- [`llms-full.txt`](llms-full.txt) — Complete API signatures and usage examples
- [`AGENTS.md`](AGENTS.md) — Architecture map and build commands for AI agents

## License

[MIT License](LICENSE)
