# Changelog

All notable changes are documented here. This project follows Kotlin compiler plugin versioning (plugin version ≡ Kotlin version) with the runtime library on its own cadence.

## [1.9.25] — Unreleased

**Breaking rewrite** — project renamed to `slf4j-ktx`. Old `slf4j-extensions` artifacts are retired. Not wire-compatible with pre-rewrite releases.

### Added

- `@io.github.harryjhin.slf4j.ktx.Slf4j` trigger annotation — opt-in annotation-based triggering (replaces `allClasses = true` default).
- Companion-centric synthesis — plugin injects `log` + `trace/debug/info/warn/error × 2` into the triggered class's `Companion` (or into the `object` itself for `@Slf4j object Foo`). User class body, supertypes, and constructors are never touched.
- 1-hop meta-annotation support — `@Slf4j` on a user annotation makes that annotation itself a trigger.
- `slf4j-ktx-spring-gradle-plugin` — Spring integration. Auto-applies the main plugin and adds the six stereotype FQNs (`@Component/@Controller/@Service/@Repository/@RestController/@ControllerAdvice`) as triggers.
- K2 FIR frontend — K1 and K2 both register unconditionally; the compiler picks the active frontend via `languageVersion`.
- Two-axis runtime compatibility diagnostics — `CORE_MISSING` / `CORE_TOO_OLD` / `COMPILER_TOO_OLD` reported via K1 `DeclarationChecker` and K2 `FirClassChecker`. (Version-reader manifest parsing is wired; deep diagnostics test coverage lands in a follow-up.)
- Independent runtime versioning — `slf4j-ktx-core` carries `coreVersion` (starts `0.1.0-SNAPSHOT`) separate from the compiler-plugin/Kotlin-paired `version`.

### Changed

- **Gradle plugin id**: `io.github.harryjhin.slf4j-extensions` → `io.github.harryjhin.slf4j-ktx`.
- **Runtime artifact**: `io.github.harryjhin:slf4j-extensions-runtime` → `io.github.harryjhin:slf4j-ktx-core`.
- **DSL surface**: single-option DSL. `propertyName`, `allClasses`, `packages(...)`, `excludeAnnotation(...)` all removed.
  ```kotlin
  // Before (v1):
  slf4jExtensions {
      propertyName = "log"
      allClasses = true
      annotation("com.example.Logged")
      packages("com.example.service")
  }
  // After:
  slf4jKtx {
      annotation("com.example.Logged")  // only user extension point
  }
  ```
- **Call-site scope**: logging calls now resolve to Companion members (or the `object` itself), not to instance members. Practical effect on `class Foo` is identical — Kotlin's scope rules make Companion members visible unqualified inside the enclosing class body. Nested classes, subclasses, and top-level functions must carry their own `@Slf4j` (Companion members do not inherit or leak to unrelated scopes).
- **Trigger semantics**: opt-in by annotation (no implicit whole-codebase coverage). Classes without `@Slf4j` (or a user trigger) are untouched.
- **Module layout**: nine new modules under `slf4j-ktx.*` / `slf4j-ktx-*`. See README Modules section.

### Removed

- `slf4j-extensions-*` eight legacy modules (tracked for removal in a follow-up step while both sets briefly coexist during the migration).
- Runtime `trace { }` / `info { }` etc. inline extensions on `Logger` — these are now supplied as Companion members by the compiler plugin, not as runtime library functions. `Marker`-qualified overloads and `withMDC { }` remain in the runtime (`MarkerExtensions.kt`, `MdcExtensions.kt`).

### Migration

| Old | New |
|---|---|
| `id("io.github.harryjhin.slf4j-extensions") version "1.9.25"` | `id("io.github.harryjhin.slf4j-ktx") version "1.9.25"` |
| `implementation("io.github.harryjhin:slf4j-extensions-runtime:1.9.25")` | `implementation("io.github.harryjhin:slf4j-ktx-core:0.1.0")` |
| `slf4jExtensions { allClasses = true }` (implicit opt-in) | Add `@Slf4j` to each class that needs logging |
| `slf4jExtensions { annotation("…") }` (filter) | `slf4jKtx { annotation("…") }` (trigger) |
| `slf4jExtensions { packages("…") }` | Add `@Slf4j` (or a custom meta-trigger annotation) to the relevant classes |
| Spring stereotypes auto-detected via package filter | `id("io.github.harryjhin.slf4j-ktx.spring")` |
