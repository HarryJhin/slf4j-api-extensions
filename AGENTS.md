# AGENTS.md

slf4j-extensions — Kotlin 1.x 컴파일러 플러그인. SLF4J Logger와 로깅 함수를 클래스에 자동 주입.

> **Branch:** `v1` — Kotlin 1.5–1.9 지원 (K1 only). `main`은 Kotlin 2.x (K1+K2) 별도 관리.

## Project Overview

This compiler plugin eliminates SLF4J logging boilerplate by auto-injecting a private `log: Logger` property and
10 inline logging functions (`trace`, `debug`, `info`, `warn`, `error` × 2 overloads) into classes at compile time.
Users write `trace { "message" }` directly without any Logger declaration.

### Modules

- `slf4j-extensions-common` — Shared constants: PluginKey, ConfigurationKeys, PluginNames
- `slf4j-extensions-k1` — K1 frontend: SyntheticResolveExtension (descriptor-based declaration generation)
- `slf4j-extensions-backend` — IR backend: IrVisitorVoid-based body generation
- `slf4j-extensions-cli` — Entry point: CommandLineProcessor + CompilerPluginRegistrar (`supportsK2 = false`)
- `slf4j-extensions-compiler` — Fat JAR: bundles compiler modules via embedded configuration
- `slf4j-extensions-runtime` — User runtime: inline Logger/Marker/MDC extension functions
- `slf4j-extensions-gradle-plugin` — Gradle plugin: KotlinCompilerPluginSupportPlugin + DSL
- `sample/` — Integration test: composite build verifying end-to-end plugin behavior

### Tech Stack

- Kotlin 1.9.25 (build toolchain, K1 pipeline only)
- SLF4J API 1.7.36 (minimum runtime, compatible with 2.0)
- Gradle 8.8 with Kotlin DSL
- JUnit 5 + kotlin-compiler-internal-test-framework:1.9.25 for compiler tests
- slf4j-test (valfirst 3.0.x) for runtime tests

## Architecture

```
slf4j-extensions-common/
├── Slf4jExtensionsPluginKey.kt      ← plugin key (shared constant)
├── Slf4jExtensionsPluginNames.kt    ← PLUGIN_ID, option names, defaults
└── Slf4jExtensionsConfigurationKeys.kt ← CompilerConfigurationKey instances

slf4j-extensions-k1/
└── Slf4jSyntheticResolveExtension.kt ← K1: property + function descriptors

slf4j-extensions-backend/
├── Slf4jIrGenerationExtension.kt    ← IrGenerationExtension adapter
└── Slf4jIrTransformer.kt            ← IR: backing field + getter + function bodies

slf4j-extensions-cli/
├── Slf4jExtensionsCommandLineProcessor.kt  ← CLI option parsing
├── Slf4jExtensionsCompilerPluginRegistrar.kt ← K1 + IR extension registration
└── META-INF/services/                       ← SPI service files

slf4j-extensions-compiler/
└── build.gradle.kts  ← embedded() fat JAR packaging

slf4j-extensions-runtime/
├── LoggerExtensions.kt   ← 10 inline Logger extensions (5 levels × 2)
├── MarkerExtensions.kt   ← 10 inline Marker extensions
└── MdcExtensions.kt      ← withMDC scoped utilities

slf4j-extensions-gradle-plugin/
├── Slf4jExtensionsGradlePlugin.kt     ← KotlinCompilerPluginSupportPlugin
└── Slf4jExtensionsGradleExtension.kt  ← DSL: propertyName, allClasses, annotation(), packages()
```

### Dependency Graph

```
cli → {k1, backend} → common
compiler → embedded(cli, k1, backend, common)
gradle-plugin → compileOnly(kotlin-gradle-plugin-api)
runtime → api(slf4j-api:1.7.36)
sample → includeBuild(root) + runtime + compiler
```

## Compiler Plugin Flow (K1)

```
Source → [Frontend] Slf4jSyntheticResolveExtension → [psi2ir] → [IR] Slf4jIrTransformer → Bytecode
         getSyntheticPropertiesNames()                           ├─ Create backing field
         generateSyntheticProperties() → log descriptor          ├─ Create getter body
         generateSyntheticMethods() → function descriptors       └─ Fill function bodies:
                                                                    if (isXxxEnabled) log.xxx(message())
```

## Commands

```bash
./gradlew clean build                     # Full build + all tests
./gradlew :slf4j-extensions-runtime:test  # Runtime tests (38)
./gradlew :slf4j-extensions-cli:test      # Compiler box tests (10, K1)
cd sample && ../gradlew run               # Integration test
```

## Testing

- Runtime: `slf4j-test` captures log events, JUnit 5 assertions
- Compiler: JetBrains `kotlin-compiler-internal-test-framework:1.9.25` box tests
  - `testData/box/*.kt` — `fun box(): String` returning `"OK"`
  - `K1BoxTestGenerated` → `AbstractK1BoxTest` (classic pipeline)
- Integration: `sample/` composite build with `slf4j-simple`

## Code Style

- Kotlin source in English, comments in Korean where needed
- No wildcard imports in production code
- `@OptIn` annotations at function level, not global (except `ExperimentalCompilerApi`)

## Release workflow (SNAPSHOT-first)

All Kotlin patch releases go through 3 stages. Full details: [RELEASING.md](RELEASING.md).

1. **SNAPSHOT publish** — push to `{kotlinVersion}-release` branch with
   `gradle.properties` `version={kotlinVersion}-SNAPSHOT`. The
   `publish-snapshot.yml` workflow publishes to Central Portal snapshot repo
   (`https://central.sonatype.com/repository/maven-snapshots/`). Re-pushable.
2. **Downstream verification** — real consumer project (e.g., kovo-backend)
   depends on `{kotlinVersion}-SNAPSHOT`, builds, tests, confirms.
3. **Release publish** — bump `version={kotlinVersion}` (no suffix), commit,
   tag `v{kotlinVersion}`, push both. The `publish.yml` workflow publishes
   the release to Maven Central and creates a GitHub Release.

Both workflows have safety checks that refuse the wrong version class
(SNAPSHOT vs release). Repository URL is selected in `build.gradle.kts`
by `version.endsWith("-SNAPSHOT")`.

GitHub Actions secrets live in the `maven` environment:
`SONATYPE_USERNAME`, `SONATYPE_PASSWORD`, `GPG_SECRET_KEY`,
`GPG_PASSPHRASE`. The Central Portal namespace `io.github.harryjhin`
must have "Enable SNAPSHOTs" turned on for stage 1 to succeed.
