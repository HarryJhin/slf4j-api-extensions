# Releasing slf4j-ktx

이 문서는 Kotlin 1.9.25 (현재 브랜치 `1.9.25-release`) 대상 릴리즈 프로세스를 정의한다. kotlinx-serialization 패턴을 그대로 따른다.

## 핵심 원칙

1. **각 Kotlin 패치 버전마다 독립된 release 브랜치를 만든다.** 브랜치는 해당 Kotlin 버전 전용이며, 한 번 publish되면 freeze한다.
2. **compiler plugin 아티팩트 버전 = Kotlin 버전** (예: `io.github.harryjhin:slf4j-ktx-compiler-plugin-embeddable:1.9.25` → Kotlin 1.9.25 전용).
3. **`slf4j-ktx-core`는 독립 cadence**. `coreVersion`(`gradle.properties`)로 관리. 플러그인 버전과 별개로 semantic versioning으로 움직인다 (초기 `0.1.0`).
4. **publish된 아티팩트는 수정하지 않는다.** 버그 수정이 필요하면 새 버전 번호로 재릴리즈를 검토한다.

## 배포 아티팩트

| 아티팩트 | 버전 축 | 설명 |
|---|---|---|
| `io.github.harryjhin:slf4j-ktx-core` | `coreVersion` (독립) | 런타임 라이브러리. `@Slf4j` 어노테이션 + Marker/MDC inline 확장. JAR manifest에 `Implementation-Version` + `Require-Kotlin-Version` stamp. |
| `io.github.harryjhin:slf4j-ktx-compiler-plugin-embeddable` | = Kotlin 버전 | 컴파일러 플러그인 fat JAR. Gradle 플러그인이 자동 resolve. 사용자가 직접 참조할 필요 없음. |
| `io.github.harryjhin:slf4j-ktx-gradle-plugin` | = Kotlin 버전 | 메인 Gradle 플러그인 (`slf4jKtx { … }` DSL). |
| `io.github.harryjhin:slf4j-ktx-spring-gradle-plugin` | = Kotlin 버전 | Spring 통합 플러그인 (메인 자동 apply + Spring stereotype FQN 6개). |

내부 컴파일러 모듈(`slf4j-ktx.common`/`.k1`/`.k2`/`.backend`/`.cli`)은 publish 대상 아님.

## 네이밍 규약

| 대상 | 포맷 | 예 |
|---|---|---|
| Release branch | `{kotlinVersion}-release` | `1.9.25-release` |
| Git tag | `v{kotlinVersion}` | `v1.9.25` |
| `gradle.properties` `version` | `{kotlinVersion}` | `1.9.25` (publish 시점), `1.9.25-SNAPSHOT` (브랜치 작업 중) |
| `gradle.properties` `kotlinVersion` | `{kotlinVersion}` | `1.9.25` |
| `gradle.properties` `coreVersion` | `{semver}` | `0.1.0` (릴리즈), `0.1.0-SNAPSHOT` (개발) |
| `gradle.properties` `requireKotlin` | `{kotlinVersion}` | `1.9.25` — 이 core 릴리즈가 요구하는 최소 Kotlin 컴파일러 버전 |

## 릴리즈 절차

모든 릴리즈는 **SNAPSHOT → 다운스트림 검증 → Release** 3단계를 거친다.

### 신규 Kotlin 패치 릴리즈 (예: `1.9.25`)

#### 1단계: 브랜치 준비 & SNAPSHOT 빌드

1. **브랜치 생성 또는 이동** — 해당 Kotlin 버전의 release 브랜치가 이미 있으면 checkout, 없으면 가장 가까운 호환 브랜치에서 분기.

2. **버전 설정** (`gradle.properties`)
   ```
   version=1.9.25-SNAPSHOT
   kotlinVersion=1.9.25
   coreVersion=0.1.0-SNAPSHOT
   requireKotlin=1.9.25
   ```
   Kotlin 버전과 플러그인/컴파일러-플러그인 버전은 함께 움직인다. `coreVersion`은 독립적으로, 관례상 이전 릴리즈 + `-SNAPSHOT`.

3. **빌드 그린 확보**
   ```bash
   ./gradlew clean build
   ./gradlew :slf4j-ktx.cli:test    # 박스 테스트 (K1 + K2)
   ```

#### 2단계: SNAPSHOT publish & 다운스트림 검증

4. **SNAPSHOT publish** — 브랜치를 push하면 `publish-snapshot.yml`이 자동 트리거된다.
   ```bash
   git push origin 1.9.25-release
   ```
   결과물: `io.github.harryjhin:*:1.9.25-SNAPSHOT` (플러그인/embeddable/gradle plugin) + `io.github.harryjhin:slf4j-ktx-core:{coreVersion}-SNAPSHOT` → Central Portal snapshot repo.
   SNAPSHOT은 같은 버전으로 재배포 가능하므로 수정 후 재푸시 반복 가능.

5. **다운스트림 프로젝트에서 실통합 검증** (예: kovo-backend)
   ```kotlin
   repositories {
       maven("https://central.sonatype.com/repository/maven-snapshots/")
   }
   plugins {
       id("io.github.harryjhin.slf4j-ktx") version "1.9.25-SNAPSHOT"
   }
   dependencies {
       implementation("io.github.harryjhin:slf4j-ktx-core:0.1.0-SNAPSHOT")
   }
   ```
   실제 애플리케이션 빌드·실행·테스트로 문제 없는지 확인. 문제 발견 시 브랜치 수정 → 재푸시 → 재검증.

#### 3단계: Release publish (비가역)

6. **릴리즈 버전으로 승격** (`gradle.properties`)
   ```
   version=1.9.25
   coreVersion=0.1.0        # 이 core 릴리즈에 부여할 semver
   ```

7. **태그 & publish**
   ```bash
   git commit -am "release: v1.9.25 (core 0.1.0)"
   git tag v1.9.25
   git push origin 1.9.25-release v1.9.25
   ```
   태그 push가 `publish.yml`을 트리거한다. 버전이 SNAPSHOT이면 실행 거부(safety check).

8. **브랜치 freeze**
   해당 브랜치에는 추가 커밋 금지. 문제 발견 시 새 패치 번호로 별도 release 브랜치 생성.

## `coreVersion` 승격 기준

`slf4j-ktx-core`는 Kotlin 플러그인과 독립적으로 움직인다. 승격 기준:

- **patch** (`0.1.0` → `0.1.1`): 내부 개선, breaking change 없음.
- **minor** (`0.1.x` → `0.2.0`): 새 public API 추가. 기존 호출부는 그대로 호환.
- **major** (`0.x.y` → `1.0.0`): breaking change. 플러그인의 `MINIMAL_SUPPORTED_VERSION` 상수도 함께 올려서 구버전 core 사용자가 `CORE_TOO_OLD` 진단을 받도록 한다.

`requireKotlin`은 현재 core 릴리즈가 의존하는 **최소 Kotlin 컴파일러 버전**. 플러그인이 `COMPILER_TOO_OLD` 진단을 낼 때 쓴다. 기본적으로 현재 개발 중인 Kotlin 버전과 동일하게 두고, core API가 특정 Kotlin 기능에 의존하면 그 최소 버전으로 맞춘다.

## Maven Central publish 전제 조건

- 비-SNAPSHOT 버전 publish 시 GPG 서명 필수 (`signingInMemoryKey` 또는 `useGpgCmd()`).
- Central Portal namespace `io.github.harryjhin`에서 **"Enable SNAPSHOTs"** 1회 활성화 필요 (namespaces 페이지).
- GitHub Actions 시크릿 (`maven` environment):
  - `SONATYPE_USERNAME`, `SONATYPE_PASSWORD` — Sonatype Central Portal 계정
  - `GPG_SECRET_KEY`, `GPG_PASSPHRASE` — GPG 키
- Repository URL은 `build.gradle.kts`에서 `version.endsWith("-SNAPSHOT")` 기준으로 자동 분기:
  - SNAPSHOT: `https://central.sonatype.com/repository/maven-snapshots/`
  - Release: `https://ossrh-staging-api.central.sonatype.com/service/local/staging/deploy/maven2/`
- Workflow:
  - `publish-snapshot.yml` — `*-release` 브랜치 push 시 실행. 버전이 SNAPSHOT 아니면 거부.
  - `publish.yml` — `v*` 태그 push 시 실행. 버전이 SNAPSHOT이면 거부. Central Portal 자동 배포 API 호출 + GitHub Release 생성.

## Gradle Plugin Portal

현재 Maven Central만 배포한다. Plugin Portal API Key는 미발급 상태. 발급 후 `com.gradle.plugin-publish` 재추가 + `publish.yml`에 Plugin Portal 스텝 복원.

## 참고

- 모델: [kotlinx.serialization의 Kotlin 버전별 release 브랜치 패턴](https://github.com/Kotlin/kotlinx.serialization/branches)
- 이유: 컴파일러 플러그인은 Kotlin 컴파일러 내부 API에 바인딩되므로 Kotlin 패치 단위로 별도 아티팩트가 필요하다. 런타임(`slf4j-ktx-core`)은 컴파일러 바인딩이 없어 독립 cadence로 움직인다.
