# Releasing

두 종류의 artifact가 있고 **릴리즈 정책이 서로 다름**:

| Artifact | 정식 릴리즈 | SNAPSHOT publish | 현재 정책 |
|---|---|---|---|
| `io.github.harryjhin:slf4j-ktx-core` | ✅ 진행 | ✅ | Maven Central에 semver로 정식 배포 |
| `io.github.harryjhin:slf4j-ktx-compiler-plugin-embeddable` | ❌ 연기 | ✅ | **SNAPSHOT only** |
| `io.github.harryjhin:slf4j-ktx-gradle-plugin` | ❌ 연기 | ✅ | **SNAPSHOT only** |
| `io.github.harryjhin:slf4j-ktx-spring-gradle-plugin` | ❌ 연기 | ✅ | **SNAPSHOT only** |

## 플러그인 정식 릴리즈를 연기한 이유

JetBrains가 **3rd-party Kotlin 컴파일러 플러그인에 대한 공식 IDE 통합 경로를 제공하지 않기 때문**. IntelliJ IDEA의 Kotlin plugin은 `kotlinx-serialization` / `kotlin-allopen` 등 공식 bundled 플러그인만 IDE resolver에 로드하며, 3rd-party plugin은 synthetic 멤버가 IDE에서 unresolved로 표시됨 (빌드·런타임은 정상). JetBrains 공식 문서도 "IDE integration ... is not ready for preview right now" 라고 명시.

정식 릴리즈가 있을 경우 사용자에게 "플러그인을 깔았는데 IDE가 인식 못 함" 경험을 강제하게 되므로, **JetBrains의 공식 통합 경로 (또는 커뮤니티 표준 bridge) 가 안정화될 때까지 플러그인 artifact는 preview SNAPSHOT으로만 유지**. 향후 아래 조건 중 하나가 충족되면 정식 릴리즈 재개를 검토:

- JetBrains가 3rd-party compiler plugin에 대한 stable IDE integration API 공개
- Kotlin IDE plugin 번들에 편입 (power-assert 전례)
- [Kotlin External FIR Support (KEFS)](https://plugins.jetbrains.com/plugin/26480-kotlin-external-fir-support) 같은 커뮤니티 bridge가 사실상 표준이 되고 우리 plugin도 그 포맷으로 배포

배경: [Kotlin Discussions — FIR plugin and IDE integration](https://discuss.kotlinlang.org/t/fir-plugin-and-ide-integration/29384), [KT-23696](https://youtrack.jetbrains.com/issue/KT-23696).

## 버전 축

두 가지 version을 `gradle.properties`에서 독립 관리:

| 변수 | 대상 | 예 |
|---|---|---|
| `coreVersion` | `slf4j-ktx-core` — 독립 semver cadence | `0.1.0`, `0.2.0`, `1.0.0` … |
| `version` | plugin 3종 — Kotlin 버전과 동기 (SNAPSHOT 전용) | `1.9.25-SNAPSHOT` |
| `kotlinVersion` | 빌드에 쓰는 Kotlin 버전 | `1.9.25` |
| `requireKotlin` | `slf4j-ktx-core`의 manifest `Require-Kotlin-Version` | `1.9.25` |

## Core 정식 릴리즈 절차 (3단계)

### 1. SNAPSHOT publish + 다운스트림 검증

브랜치에서 작업 후 `gradle.properties`:
```
coreVersion=0.2.0-SNAPSHOT
```

`1.9.25-release` 브랜치 push → `publish-snapshot.yml` 자동 트리거 → Central Portal snapshot repo에 `slf4j-ktx-core:0.2.0-SNAPSHOT` (+ plugin 3종 SNAPSHOT도 함께) 업로드.

다운스트림(kovo-backend 등)에서 SNAPSHOT 의존성으로 빌드·테스트. 이슈 발견 시 브랜치 수정 → 재푸시 (SNAPSHOT은 overwrite 가능).

### 2. Release 버전 승격

`gradle.properties`:
```
coreVersion=0.2.0
```

`requireKotlin`은 core API가 특정 Kotlin 기능에 의존하면 그에 맞춰 업데이트.

### 3. Tag + publish

```bash
git commit -am "release: core 0.2.0"
git tag core-v0.2.0
git push origin 1.9.25-release core-v0.2.0
```

`core-v*` tag가 `publish.yml`을 트리거 → `:slf4j-ktx-core:publish` 만 실행 → Maven Central staging repo로 업로드 → Central Portal에서 자동 승격.

Plugin 3종은 이 tag 플로우에서 **publish되지 않음**.

## Plugin SNAPSHOT publish 절차

Plugin SNAPSHOT은 **`1.9.25-release` 브랜치 push 트리거로만 배포**. 정식 release tag는 만들지 않음.

`gradle.properties`의 `version=1.9.25-SNAPSHOT` 유지 상태에서 `publish-snapshot.yml`이 4개 artifact 전부 Central Portal snapshot repo로 push. Plugin 3종은 ABI가 Kotlin 내부 API에 바인딩되므로 버전은 Kotlin 버전과 동기한 `SNAPSHOT`으로만 사용.

## Maven Central publish 전제 조건

- Release 버전(non-SNAPSHOT) publish 시 GPG 서명 필수 (`signingInMemoryKey` 또는 `useGpgCmd()`).
- Central Portal namespace `io.github.harryjhin` 에서 "Enable SNAPSHOTs" 1회 활성화 (SNAPSHOT publish 전제).
- GitHub Actions secrets (`maven` environment): `SONATYPE_USERNAME`, `SONATYPE_PASSWORD`, `GPG_SECRET_KEY`, `GPG_PASSPHRASE`.
- Repository URL은 `build.gradle.kts`에서 `version.endsWith("-SNAPSHOT")` 기준 자동 분기:
  - SNAPSHOT: `https://central.sonatype.com/repository/maven-snapshots/`
  - Release: `https://ossrh-staging-api.central.sonatype.com/service/local/staging/deploy/maven2/`

## 참고

- 모델: [kotlinx-serialization](https://github.com/Kotlin/kotlinx.serialization) 은 runtime과 compiler plugin을 **물리적 repo 분리**로 독립 cadence 관리. 우리는 단일 repo이므로 **release 절차만 분리**(core만 release tag 사용).
- 컴파일러 플러그인은 Kotlin 컴파일러 내부 API에 바인딩되므로 Kotlin 패치 단위로 별도 아티팩트가 필요. 런타임(`slf4j-ktx-core`)은 컴파일러 바인딩이 없어 독립 cadence 가능.
