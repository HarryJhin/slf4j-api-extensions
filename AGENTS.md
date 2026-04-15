# AGENTS.md

slf4j-extensions — Kotlin compiler plugin that automatically injects SLF4J Logger and logging functions into classes.

## Project Overview

This compiler plugin eliminates SLF4J logging boilerplate by auto-injecting a private `log: Logger` property and
10 inline logging functions (`trace`, `debug`, `info`, `warn`, `error` × 2 overloads) into classes at compile time.
Users write `trace { "message" }` directly without any Logger declaration.

### Modules

- `slf4j-extensions-common` — Shared constants: PluginKey, ConfigurationKeys, PluginNames
- `slf4j-extensions-k1` — K1 frontend: SyntheticResolveExtension (descriptor-based declaration generation)
- `slf4j-extensions-k2` — K2 FIR frontend: FirDeclarationGenerationExtension (FIR-based declaration generation)
- `slf4j-extensions-backend` — IR backend: IrVisitorVoid-based body generation (shared by K1 and K2)
- `slf4j-extensions-cli` — Entry point: CommandLineProcessor + CompilerPluginRegistrar
- `slf4j-extensions-compiler` — Fat JAR: bundles all compiler modules via embedded configuration
- `slf4j-extensions-runtime` — User runtime: inline Logger/Marker/MDC extension functions
- `slf4j-extensions-gradle-plugin` — Gradle plugin: KotlinCompilerPluginSupportPlugin + DSL
- `sample/` — Integration test: composite build verifying end-to-end plugin behavior

### Tech Stack

- Kotlin 2.3.20 (build toolchain, supports K1 and K2 pipelines)
- SLF4J API 1.7.36 (minimum runtime, compatible with 2.0)
- Gradle 8.8 with Kotlin DSL
- JUnit 5 + kotlin-compiler-internal-test-framework for compiler tests
- slf4j-test (valfirst 3.0.x) for runtime tests

## Architecture

```
slf4j-extensions-common/
├── Slf4jExtensionsPluginKey.kt      ← GeneratedDeclarationKey (K2 origin tagging)
├── Slf4jExtensionsPluginNames.kt    ← PLUGIN_ID, option names, defaults
└── Slf4jExtensionsConfigurationKeys.kt ← CompilerConfigurationKey instances

slf4j-extensions-k1/
└── Slf4jSyntheticResolveExtension.kt ← K1: property + function descriptors

slf4j-extensions-k2/
├── FirSlf4jExtensionRegistrar.kt     ← FIR extension factory
└── FirSlf4jDeclarationGenerator.kt   ← FIR: log property + 10 inline functions

slf4j-extensions-backend/
├── Slf4jIrGenerationExtension.kt    ← IrGenerationExtension adapter
└── Slf4jIrTransformer.kt            ← IR: fills property initializer + function bodies

slf4j-extensions-cli/
├── Slf4jExtensionsCommandLineProcessor.kt  ← CLI option parsing
├── Slf4jExtensionsCompilerPluginRegistrar.kt ← K1 + K2 + IR extension registration
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
cli → {k1, k2, backend} → common
compiler → embedded(cli, k1, k2, backend, common)
gradle-plugin → compileOnly(kotlin-gradle-plugin-api)
runtime → api(slf4j-api:1.7.36)
sample → includeBuild(root) + runtime + compiler
```

## Compiler Plugin Flow

### K2 Path (Kotlin 2.0+)

```
Source → [FIR] FirSlf4jDeclarationGenerator → [IR] Slf4jIrTransformer → Bytecode
         getCallableNamesForClass()              visitProperty() → LoggerFactory.getLogger()
         generateProperties() → log: Logger      visitSimpleFunction() → if (isXxxEnabled) log.xxx(message())
         generateFunctions() → trace/debug/...
```

### K1 Path (Kotlin 1.x)

```
Source → [Frontend] Slf4jSyntheticResolveExtension → [psi2ir] → [IR] Slf4jIrTransformer → Bytecode
         getSyntheticPropertiesNames()                           Same as K2, plus:
         generateSyntheticProperties() → log descriptor          - Create backing field
         generateSyntheticMethods() → function descriptors       - Create getter body
```

## Commands

```bash
./gradlew clean build                     # Full build + all tests
./gradlew :slf4j-extensions-runtime:test  # Runtime tests (38)
./gradlew :slf4j-extensions-cli:test      # Compiler box tests (20: K1×10 + K2×10)
cd sample && ../gradlew run               # Integration test
```

## Testing

- Runtime: `slf4j-test` captures log events, JUnit 5 assertions
- Compiler: JetBrains `kotlin-compiler-internal-test-framework` box tests
  - `testData/box/*.kt` — `fun box(): String` returning `"OK"`
  - K2: `K2BoxTestGenerated` → `AbstractK2BoxTest` (FIR pipeline)
  - K1: `K1BoxTestGenerated` → `AbstractK1BoxTest` (classic pipeline)
  - Both use same testData, different pipelines
- Integration: `sample/` composite build with `slf4j-simple`

## Code Style

- Kotlin source in English, comments in Korean where needed
- No wildcard imports in production code
- `@OptIn` annotations at function level, not global (except `ExperimentalCompilerApi` and `UnsafeDuringIrConstructionAPI`)
