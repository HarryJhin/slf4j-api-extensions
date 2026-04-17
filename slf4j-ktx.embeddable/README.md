# slf4j-ktx.embeddable

**사용자용 fat JAR** — 컴파일러 플러그인의 모든 내부 모듈과 필요 의존성을 하나의 JAR로 합치되, Kotlin compiler internals (`com.intellij`, `asm`, `protobuf` 등)를 shaded 패키지로 relocate하여 사용자 프로젝트의 Kotlin 컴파일러 classpath와 **충돌하지 않게** 만든다. `kotlin-stdlib` / `annotations` 같은 공유 의존은 relocate 대신 exclude한다 (사용자 프로젝트와 공유).

## 존재 이유

컴파일러 플러그인은 JVM classloader 상에서 **사용자의 Kotlin 컴파일러와 동일한 런타임**에 로드된다. 플러그인이 번들한 컴파일러 내부 클래스가 사용자 측과 버전 mismatch를 일으키면 `NoSuchMethodError` 또는 상수 mismatch로 실패한다. Shaded JAR은 이 문제를 원천 차단한다.

serialization은 `rewriteDefaultJarDepsToShadedCompiler()`라는 **JetBrains 내부 buildSrc 헬퍼**로 동일한 효과를 낸다. 우리는 외부 프로젝트이므로 공개 플러그인 `com.gradleup.shadow`로 같은 relocation 규칙을 수동 구현한다.

## 주요 구성 (Step 9에서 구현)

- `com.gradleup.shadow` 플러그인 적용
- `embedded` configuration으로 내부 모듈 번들 (`:slf4j-ktx.cli`, `.common`, `.k1`, `.k2`, `.backend`)
- Relocation 규칙: `com.intellij`, `org.jetbrains.kotlin.com.intellij`, `org.objectweb.asm`, `org.jetbrains.kotlin.org.objectweb.asm`, `com.google.protobuf`, `it.unimi.dsi.fastutil`, `kotlinx.collections.immutable` → `io.github.harryjhin.slf4j.ktx.shaded.*`
- Exclude: `kotlin-stdlib(-jdk7/8)`, `kotlin-reflect`, `org.jetbrains:annotations`
- `publishing`: artifactId `slf4j-ktx-compiler-plugin-embeddable`

## 의존

- `implementation(project(":slf4j-ktx.cli"))` 및 내부 모듈 (shadow가 한 JAR로 병합)

## 참조

- SPEC §4.1, §5.7, §9.3
- serialization 대응: `kotlinx-serialization.embeddable/build.gradle.kts` — `rewriteDefaultJarDepsToShadedCompiler()` 효과의 공개-플러그인 재현
