# AGENTS.md

slf4j-ktx — Kotlin 1.9.25 compiler plugin. Injects SLF4J Logger + level functions onto the `Companion` of annotated classes (or onto `@Slf4j object`s themselves).

> **Branch:** `1.9.25-release` — active rewrite from `slf4j-extensions`. Legacy `slf4j-extensions-*` modules are still checked in and will be removed in a follow-up. See [CHANGELOG.md](CHANGELOG.md) + [docs/REWRITE-PROGRESS.md](docs/REWRITE-PROGRESS.md).

## Project Overview

The plugin's promise: the user writes `@Slf4j class Foo { fun f() { info { "…" } } }` and the compiler emits `Foo.Companion` with a `log: Logger` property and ten level functions. The user class body — its supertypes, constructors, direct members — is never modified. This mirrors kotlinx-serialization, which puts `serializer()` on the Companion without touching the serializable class itself.

## Modules

### slf4j-ktx (active)

- `slf4j-ktx.common` — shared plugin constants (`Slf4jKtxPluginKey`, `Slf4jKtxConfig`, entity/plugin names, version floors).
- `slf4j-ktx.k1` — K1 frontend: `SyntheticResolveExtension` for Companion synthesis + level-function descriptors, `DescriptorSerializerPlugin` hook, `DeclarationChecker` for runtime-version diagnostics.
- `slf4j-ktx.k2` — K2 FIR frontend: `FirDeclarationGenerationExtension` for Companion + callables, `FirAdditionalCheckersExtension` for diagnostics, session-scoped `FirExtensionSessionComponent` for manifest version caching.
- `slf4j-ktx.backend` — IR lowering: fills Companion-member bodies (`if (log.isXxxEnabled) log.xxx(message(), throwable?)`) and initializes the `log` backing field via `LoggerFactory.getLogger(FQN)`.
- `slf4j-ktx.cli` — aggregator: Register-All `CompilerPluginRegistrar` + single-option `CommandLineProcessor` (`annotation` FQN, multi-valued).
- `slf4j-ktx.embeddable` — fat JAR for end-user consumption. `embedded` Gradle configuration + `zipTree`; no shadow plugin (compileOnly deps keep compiler internals out of the JAR).
- `slf4j-ktx-core` — runtime library (published). `@Slf4j` annotation + Marker/MDC inline extensions. Independent `coreVersion` cadence; JAR manifest stamps `Implementation-Version` + `Require-Kotlin-Version` for two-axis compatibility checking.
- `slf4j-ktx-gradle-plugin` — main Gradle plugin (`slf4jKtx { annotation("…") }` DSL, `SubpluginArtifact` pointing at `slf4j-ktx-compiler-plugin-embeddable`).
- `slf4j-ktx-spring-gradle-plugin` — `Plugin<Project>` that auto-applies the main plugin and pushes six Spring stereotype FQNs into `Slf4jKtxGradleExtension.myAnnotations`. No separate `KotlinCompilerPluginSupportPlugin` — follows `kotlin-spring`'s `KotlinSpringSubplugin` pattern.

### slf4j-extensions (legacy, scheduled for removal)

Eight `slf4j-extensions-*` modules from the pre-rewrite era. They co-exist during the rewrite window and will be removed in a dedicated cleanup step.

### Tech Stack

- Kotlin 1.9.25 (build + K1/K2 target)
- SLF4J API 1.7.36 (min; compatible with 2.0)
- Gradle 8.8 with Kotlin DSL
- JUnit 5 + `kotlin-compiler-internal-test-framework:1.9.25` for box tests
- slf4j-test (valfirst 3.0.x) for runtime unit tests

## Dependency Graph

```
slf4j-ktx.common     (leaf — compileOnly compiler deps)
slf4j-ktx.k1         → common
slf4j-ktx.k2         → common
slf4j-ktx.backend    → common
slf4j-ktx.cli        → common, k1, k2, backend   (registrar + CLI options)
slf4j-ktx.embeddable → embedded(common, k1, k2, backend, cli)   (fat JAR)

slf4j-ktx-core                    → api(slf4j-api:1.7.36)
slf4j-ktx-gradle-plugin           → compileOnly(kotlin-gradle-plugin-api)
slf4j-ktx-spring-gradle-plugin    → implementation(slf4j-ktx-gradle-plugin) +
                                    compileOnly(kotlin-gradle-plugin-api)
```

## Compiler Plugin Flow

K1 and K2 are both registered unconditionally by `Slf4jKtxComponentRegistrar` (`supportsK2 = true`). The compiler picks the active frontend by `languageVersion`.

### K1

```
SyntheticResolveExtension
├─ getSyntheticCompanionObjectNameIfNeeded → auto-create Companion when absent
├─ getSyntheticPropertiesNames/FunctionNames → announce "log" + 5 level names
├─ generateSyntheticProperties → log PropertyDescriptor (via Slf4jKtxDescriptorResolver)
└─ generateSyntheticMethods → 5 × 2 level function descriptors

DescriptorSerializerPlugin (empty; reserved for phantom-Companion filtering)
StorageComponentContainerContributor → DeclarationChecker → runtime-version diagnostics
```

### K2

```
FirDeclarationGenerationExtension
├─ getNestedClassifiersNames → announce Companion
├─ generateNestedClassLikeDeclaration → createCompanionObject(key = Slf4jKtxPluginKey)
├─ getCallableNamesForClass → "log" + 5 level names; also SpecialNames.INIT for plugin-synthesized Companion
├─ generateProperties → log FirPropertySymbol (hasBackingField = true)
├─ generateFunctions → level FirNamedFunctionSymbols
└─ generateConstructors → createDefaultPrivateConstructor for plugin-synthesized Companion
  (required; ObjectClassLowering fails otherwise — "Object should have a primary constructor")

FirAdditionalCheckersExtension → FirClassChecker → runtime-version diagnostics
FirExtensionSessionComponent → FirSlf4jKtxVersionReader (session-cached manifest probe)
```

### IR

```
IrGenerationExtension (Slf4jKtxLoweringExtension)
└─ IrElementVisitorVoid (Slf4jKtxIrGenerator)
    ├─ visitProperty → if isFromPlugin(afterK2): create backing field, init to LoggerFactory.getLogger(FQN), fill getter
    └─ visitSimpleFunction → if isFromPlugin: body = if (log.isXxxEnabled) log.xxx(message.invoke()[, throwable])

origin signal:
  K1:  CallableMemberDescriptor.Kind.SYNTHESIZED
  K2:  IrDeclarationOrigin.GeneratedByPlugin(Slf4jKtxPluginKey)

logger name:
  site is Companion → enclosing class FQN
  site is @Slf4j object → object's own FQN
```

## Commands

```bash
./gradlew clean build                              # Full build
./gradlew :slf4j-ktx-core:test                     # Runtime unit tests
./gradlew :slf4j-ktx.cli:test                      # Box tests: 26 (K1 13 + K2 13)
./gradlew :slf4j-ktx-gradle-plugin:jar             # Gradle plugin JAR + descriptor
./gradlew :slf4j-ktx-spring-gradle-plugin:jar      # Spring Gradle plugin JAR + descriptor
./gradlew :slf4j-ktx.embeddable:jar                # Fat JAR (compiler plugin)
```

Sample integration (`sample/`) is scheduled to switch to the new plugin in a follow-up step; until then it exercises the legacy modules.

## Testing

- **Box tests** (`slf4j-ktx.cli:test`):
  - Fixtures under `testData/box/*.kt` (root of the repo). Each contains `fun box(): String` that must return `"OK"`.
  - Runners in `testFixtures/kotlin/.../runners/` (root of the repo): `AbstractSlf4jKtxK1BoxTest` extends `AbstractIrBlackBoxCodegenTest`; `AbstractSlf4jKtxFirLightTreeBoxTest` extends `AbstractFirLightTreeBlackBoxCodegenTest`.
  - Generated `@TestMetadata` classes in `tests-gen/kotlin/.../runners/` are hand-written (the `generateTestGroupSuiteWithJUnit5` DSL ships in JetBrains' monorepo-only `tests-gen` module, not on Maven Central).
  - 26 tests total: 13 fixtures × 2 frontends.
- **Runtime unit tests** (`slf4j-ktx-core:test`): slf4j-test + JUnit 5 for the `@Slf4j` annotation and Marker/MDC utilities.

## Code Style

- Kotlin source in English, Korean where a domain note is clearer.
- No wildcard imports in production code.
- `@OptIn(ExperimentalCompilerApi)` at declaration level; applied globally via `kotlin { compilerOptions { optIn.add("…") } }` in the build for compiler-facing modules.

## Consumer-side setup

The Gradle plugin does **not** auto-add `slf4j-ktx-core`. Declare it explicitly — same convention as kotlinx-serialization, KSP, etc. The compiler-plugin artifact (`slf4j-ktx-compiler-plugin-embeddable`) is resolved by the Kotlin Gradle plugin automatically; consumers never reference it directly.

```kotlin
plugins {
    kotlin("jvm") version "1.9.25"
    id("io.github.harryjhin.slf4j-ktx") version "1.9.25"
}
dependencies {
    implementation("io.github.harryjhin:slf4j-ktx-core:0.1.0")
}
```

## Release workflow (SNAPSHOT-first)

Three stages per Kotlin patch release. Full details: [RELEASING.md](RELEASING.md).

1. **SNAPSHOT publish** — push to `{kotlinVersion}-release` with `version={kotlinVersion}-SNAPSHOT`. `publish-snapshot.yml` publishes to Central Portal snapshot repo. Re-pushable.
2. **Downstream verification** — a real consumer (e.g. kovo-backend) depends on the SNAPSHOT, builds, tests, confirms.
3. **Release publish** — bump `version={kotlinVersion}`, commit, tag `v{kotlinVersion}`, push both. `publish.yml` publishes to Maven Central and creates a GitHub Release. Safety check refuses SNAPSHOT inputs.

GitHub Actions secrets in the `maven` environment: `SONATYPE_USERNAME`, `SONATYPE_PASSWORD`, `GPG_SECRET_KEY`, `GPG_PASSPHRASE`. Central Portal namespace `io.github.harryjhin` must have "Enable SNAPSHOTs" turned on.
