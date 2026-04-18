# slf4j-ktx-spring-gradle-plugin

Optional Gradle plugin adding Spring stereotype triggers on top of [`slf4j-ktx-gradle-plugin`](../slf4j-ktx-gradle-plugin). **Preview — SNAPSHOT-only.**

## What it does

1. Auto-applies `slf4j-ktx-gradle-plugin`.
2. Registers six Spring stereotype FQNs as additional triggers, so Spring-managed classes participate in the IR call-site rewriter without needing a separate `@Slf4j` annotation.

The compiler plugin itself is registered exactly once — by the main plugin. This plugin's only effect is to mutate the shared `Slf4jKtxGradleExtension.myAnnotations` list; Kotlin's `kotlin-spring` + `kotlin-allopen` pairing uses the same approach.

## Applying

### settings.gradle.kts

```kotlin
pluginManagement {
    repositories {
        gradlePluginPortal()
        maven("https://central.sonatype.com/repository/maven-snapshots/") {
            content {
                includeModule(
                    "io.github.harryjhin.slf4j-ktx.spring",
                    "io.github.harryjhin.slf4j-ktx.spring.gradle.plugin"
                )
                includeModule(
                    "io.github.harryjhin.slf4j-ktx",
                    "io.github.harryjhin.slf4j-ktx.gradle.plugin"
                )
                includeModule("io.github.harryjhin", "slf4j-ktx-spring-gradle-plugin")
                includeModule("io.github.harryjhin", "slf4j-ktx-gradle-plugin")
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
    id("io.github.harryjhin.slf4j-ktx.spring") version "1.9.25-SNAPSHOT"
}

dependencies {
    implementation("io.github.harryjhin:slf4j-ktx-core:0.1.0")
}
```

Applying this plugin also applies `slf4j-ktx-gradle-plugin` — no need to list both.

## Triggers registered

- `org.springframework.stereotype.Component`
- `org.springframework.stereotype.Controller`
- `org.springframework.stereotype.Service`
- `org.springframework.stereotype.Repository`
- `org.springframework.web.bind.annotation.RestController`
- `org.springframework.web.bind.annotation.ControllerAdvice`

These compose with `@Slf4j` and with any FQNs added via `slf4jKtx { annotation("…") }`.

## Usage

```kotlin
import io.github.harryjhin.slf4j.ktx.*
import org.springframework.stereotype.Service

@Service
class OrderService {
    fun process() {
        info { "processing" }
    }
}
```

No `@Slf4j` needed — `@Service` already qualifies as a trigger.

## Not covered by the default list

Several Spring-managed classes carry the core `@Component` stereotype at two or more meta-annotation hops rather than directly:

- `@Configuration` → `@Component`
- `@AutoConfiguration` → `@Configuration` → `@Component`
- `@SpringBootApplication` → `@SpringBootConfiguration` → `@Configuration` → `@Component`
- `@ConfigurationProperties` (standalone — not a stereotype at all)

The plugin's meta-annotation predicate is **1-hop only**, so these chains are not auto-registered. Register explicitly when needed:

```kotlin
slf4jKtx {
    annotation("org.springframework.context.annotation.Configuration")
    annotation("org.springframework.boot.autoconfigure.AutoConfiguration")
    annotation("org.springframework.boot.autoconfigure.SpringBootApplication")
    annotation("org.springframework.boot.context.properties.ConfigurationProperties")
}
```

## IDE limitations

Same constraints as the main plugin — see [slf4j-ktx-gradle-plugin/README.md](../slf4j-ktx-gradle-plugin/README.md#ide-limitations).

## License

[MIT License](../LICENSE)
