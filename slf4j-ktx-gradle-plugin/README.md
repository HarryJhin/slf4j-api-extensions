# slf4j-ktx-gradle-plugin

Optional Gradle plugin for slf4j-ktx. **Preview — SNAPSHOT-only.** See the root [RELEASING.md](../RELEASING.md) for why the plugin track is parked on SNAPSHOT.

## What it does

Applies the slf4j-ktx compiler plugin and:

1. Synthesizes a `log: Logger` property on the Companion (or directly on an `object`) of each triggered class, initialized with `LoggerFactory.getLogger(<class FQN>)`.
2. Rewrites each call to the runtime `T.trace / T.debug / T.info / T.warn / T.error` extensions made from inside a triggered class into a direct static-field read — no per-call `LoggerFactory.getLogger` cache lookup.

Without this plugin, [`slf4j-ktx-core`](../slf4j-ktx-core) works end-to-end as a runtime-only logging library. The plugin is a performance optimization plus a trigger-annotation mechanism.

## Applying

### settings.gradle.kts

```kotlin
pluginManagement {
    repositories {
        gradlePluginPortal()
        maven("https://central.sonatype.com/repository/maven-snapshots/") {
            content {
                includeModule("io.github.harryjhin.slf4j-ktx", "io.github.harryjhin.slf4j-ktx.gradle.plugin")
                includeModule("io.github.harryjhin", "slf4j-ktx-gradle-plugin")
                includeModule("io.github.harryjhin", "slf4j-ktx-compiler-plugin-embeddable")
            }
        }
    }
}

dependencyResolutionManagement {
    repositories {
        mavenCentral()
        maven("https://central.sonatype.com/repository/maven-snapshots/") {
            content {
                includeModule("io.github.harryjhin", "slf4j-ktx-compiler-plugin-embeddable")
            }
        }
    }
}
```

### build.gradle.kts

```kotlin
plugins {
    kotlin("jvm") version "1.9.25"
    id("io.github.harryjhin.slf4j-ktx") version "1.9.25-SNAPSHOT"
}

dependencies {
    implementation("io.github.harryjhin:slf4j-ktx-core:0.1.0")
}
```

The plugin version must match the Kotlin version you're building with — the compiler-plugin ABI is pinned to Kotlin compiler internals.

## Usage

```kotlin
import io.github.harryjhin.slf4j.ktx.*

@Slf4j
class OrderService {
    fun process(order: Order) {
        info { "processing: ${order.id}" }
        error(exception) { "failed" }
    }
}
```

Inside `OrderService` both calls become `OrderService.Companion.log.info(...)` / `.error(...)` at IR lowering time — no cache lookup, direct static-field read.

## DSL

```kotlin
slf4jKtx {
    annotation("com.example.LoggedDomain")
    annotation("org.springframework.context.annotation.Configuration")
}
```

- Single option: `annotation(fqName)`, repeatable.
- Entries accumulate with the built-in `@Slf4j` trigger.
- When `slf4j-ktx-spring-gradle-plugin` is applied alongside, Spring stereotype FQNs are added on top of these.

## Trigger semantics

A class is a rewrite target when at least one of its annotations is a registered trigger. Annotations eligible:

- Built-in: `@io.github.harryjhin.slf4j.ktx.Slf4j` (shipped in `slf4j-ktx-core`).
- DSL-registered: any FQN passed to `slf4jKtx { annotation("…") }`.
- Meta-annotation (1-hop): if `X` is a trigger and `@X annotation class Y`, then `@Y class Foo` triggers too. Does **not** chain further — `X → Z → Y → class` (2+ hops) is not recognized.

Skipped unconditionally: `interface`, `annotation class`, local / anonymous classes.

## IDE limitations

Third-party Kotlin compiler plugins are not recognized by IntelliJ IDEA's Kotlin resolver. Practical consequences:

- The synthetic `log: Logger` on `Companion` is invisible to the IDE. Code like `log.info("literal {}", value)` written inside a `@Slf4j` class appears as an unresolved reference in the editor, even though compilation and runtime succeed.
- The IR call-site rewriter is post-frontend, so the call to `info { }` itself is still resolved by the IDE against the real top-level extension in `slf4j-ktx-core` — the rewrite is invisible. Red squiggles, if any, are confined to direct uses of the `Companion.log` property.

See the [root README "Design note"](../README.md#design-note-why-a-compiler-plugin) for the long-form reasoning and upstream tracking.

## License

[MIT License](../LICENSE)
