# Changelog

Release history for the `io.github.harryjhin:slf4j-ktx-core` artifact, versioned on its own cadence (`coreVersion` in `gradle.properties`) — independent of the Kotlin compiler version the repo is paired with.

> Compiler-plugin artifacts (`slf4j-ktx-compiler-plugin-embeddable`, `slf4j-ktx-gradle-plugin`, `slf4j-ktx-spring-gradle-plugin`) are **SNAPSHOT-only**; they do not appear in this log. Rationale and workflow: [RELEASING.md](RELEASING.md).

The format is based on [Keep a Changelog](https://keepachangelog.com/); this project adheres to [Semantic Versioning](https://semver.org/).

## [0.1.0] — Unreleased

Initial public release.

### Added

- `@io.github.harryjhin.slf4j.ktx.Slf4j` trigger annotation. Opt-in marker consumed by the optional compiler plugin; a plain runtime annotation otherwise.
- Top-level inline extensions `T.trace / T.debug / T.info / T.warn / T.error` with message-only `(() -> String)` and throwable-aware `(Throwable, () -> String)` overloads. Each is level-gated and evaluates the message lambda only when the level is enabled. Default body resolves the logger through SLF4J's `LoggerFactory.getLogger(T::class.java)` cache.
- `Logger.<level>(Marker, …)` inline extensions — Marker-qualified variants with the same lazy-lambda + level-gate shape.
- `withMDC(vararg Pair<String, String>, block)` and `withMDC(Map<String, String>, block)` — scoped MDC mutation that restores prior values on exit (including on exception).

### JAR manifest

The published JAR stamps two attributes consumed by the optional compiler plugin's version reader:

- `Implementation-Version` — the `coreVersion` of this release.
- `Require-Kotlin-Version` — the minimum Kotlin compiler version this release requires.

### Compatibility

| Requirement | Version |
|---|---|
| Kotlin runtime target | 1.9.25+ |
| Java | 8+ |
| SLF4J | 1.7.36+ (compatible with 2.0) |
