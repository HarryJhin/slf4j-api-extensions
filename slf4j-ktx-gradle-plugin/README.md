# slf4j-ktx-gradle-plugin

**메인 Gradle 플러그인** — 사용자 Gradle 빌드에서 `id("io.github.harryjhin.slf4j-ktx")`로 적용하면 컴파일러 플러그인을 `-Xplugin`으로 자동 연결하고 `slf4jKtx { }` DSL을 노출한다.

## 존재 이유

Gradle 빌드에서 플러그인 artifact 선언, `-Xplugin` 플래그 추가, CLI 옵션 전달을 수동으로 하려면 복잡하다. `KotlinCompilerPluginSupportPlugin` 인터페이스가 이 과정을 캡슐화한다. `apply` 시점에 `Slf4jKtxGradleExtension`을 생성하고, `applyToCompilation` 시점에 사용자의 `annotation("...")` 호출 목록을 `SubpluginOption`으로 변환하여 컴파일러에 전달한다.

DSL은 kotlin-allopen 관례 그대로 — **eager `mutableListOf<String>` + `annotation(fqName: String)` 메서드** (`libraries/tools/kotlin-allopen/src/common/kotlin/.../AllOpenExtension.kt:19-37`). `ListProperty`를 쓰지 않는 이유는 Gradle 구성 캐시와의 호환성 + allopen과의 일관성.

## 주요 타입 (Step 10에서 구현)

- `Slf4jKtxGradleExtension` — `internal val myAnnotations = mutableListOf<String>()` + `open fun annotation(fqName: String)`
- `Slf4jKtxGradleSubplugin` — `KotlinCompilerPluginSupportPlugin` 구현. `getCompilerPluginId()`, `getPluginArtifact()`, `applyToCompilation()`. Extension 이름 `"slf4jKtx"`, 플러그인 id `"io.github.harryjhin.slf4j-ktx"`, artifact `slf4j-ktx-compiler-plugin-embeddable`

## 의존

- `compileOnly`: `kotlin-gradle-plugin-api`, `kotlin-gradle-plugin`
- 런타임 artifact 목적의 plugin descriptor: `gradlePlugin { plugins { create("slf4jKtx") { ... } } }`

## 참조

- SPEC §4.1, §5.9, §6.7
- 대응: `libraries/tools/kotlin-allopen/src/common/kotlin/.../AllOpenSubplugin.kt` (`applyToCompilation` 패턴), `AllOpenExtension.kt` (DSL 모양)
