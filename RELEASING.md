# Releasing slf4j-extensions (v1 branch)

이 문서는 Kotlin 1.x 대상 릴리즈 프로세스를 정의한다. kotlinx-serialization 패턴을 그대로 따른다.

## 핵심 원칙

1. **v1 HEAD는 현재 시점에서 지원하는 가장 최신 Kotlin 1.x 버전만 추적한다.** 레거시 호환 코드를 HEAD에 남기지 않는다.
2. **각 Kotlin 패치 버전마다 독립된 release 브랜치를 만든다.** 브랜치는 해당 Kotlin 버전 전용이며, 한 번 publish되면 freeze한다.
3. **plugin 아티팩트 버전 = Kotlin 버전** (예: `io.github.harryjhin:slf4j-extensions-runtime:1.9.25` → Kotlin 1.9.25 전용).
4. **publish된 아티팩트는 수정하지 않는다.** 버그 수정이 필요하면 새 버전 번호(예: Kotlin 1.9.25-1, 별도 tag)로 재릴리즈를 검토한다.

## 네이밍 규약

| 대상 | 포맷 | 예 |
|------|------|------|
| Release branch | `{kotlinVersion}-release` | `1.5.32-release`, `1.9.25-release` |
| Git tag | `v{kotlinVersion}` | `v1.5.32`, `v1.9.25` |
| `gradle.properties` `version` | `{kotlinVersion}` | `1.9.25` (publish 시점), `1.9.25-SNAPSHOT` (브랜치 작업 중) |
| `gradle.properties` `kotlinVersion` | `{kotlinVersion}` | `1.9.25` |

## 릴리즈 절차

### 신규 Kotlin 패치 릴리즈 (예: `1.9.25`)

1. **브랜치 생성**
   ```bash
   git checkout v1
   git checkout -b 1.9.25-release
   ```
   v1 HEAD에서 직접 분기하는 것이 원칙. 단, HEAD가 더 높은 Kotlin 버전(예: 새 minor)으로 이동한 상태에서 과거 Kotlin 버전을 릴리즈해야 한다면 가장 가까운 호환 release 브랜치(예: `1.9.24-release`)에서 분기한다.

2. **버전 설정**
   `gradle.properties`
   ```
   version=1.9.25-SNAPSHOT
   kotlinVersion=1.9.25
   ```

3. **빌드 그린 확보**
   ```bash
   ./gradlew clean build -PkotlinVersion=1.9.25
   cd sample && ../gradlew run && cd ..
   ```
   Kotlin 버전에 따라 컴파일러 API가 달라지면 이 브랜치에서만 수정한다. v1 HEAD에는 반영하지 않는다.

4. **릴리즈 버전으로 승격**
   ```
   version=1.9.25
   ```

5. **태그 & publish**
   ```bash
   git commit -am "release: v1.9.25"
   git tag v1.9.25
   git push origin 1.9.25-release v1.9.25
   ```
   태그 push가 publish workflow를 트리거한다 (Infra #1 구현 후).

6. **브랜치 freeze**
   해당 브랜치에는 추가 커밋 금지. 문제 발견 시 새 패치 번호로 별도 release 브랜치 생성.

### 과거 Kotlin 버전 릴리즈 (예: `1.5.0`을 나중에 지원 추가)

1. 위 "신규 패치 릴리즈" 1~6 단계 동일. 단 1번에서 가장 가까운 호환 release 브랜치(동일 minor의 다른 패치)에서 분기하거나, 없으면 v1 HEAD에서 분기 후 해당 Kotlin 버전에 맞게 코드를 소급 수정.
2. API 차이로 수정이 커지면 해당 release 마일스톤 이슈에 차이점을 기록한다.

## v1 HEAD 이동 규칙

- JetBrains가 새로운 Kotlin 1.x 패치(예: `1.9.26`)를 릴리즈하면:
  1. v1 HEAD에서 `1.9.26-release` 브랜치 생성
  2. v1 HEAD의 `kotlinVersion`을 `1.9.26`으로 갱신
  3. 새 패치 릴리즈 절차 따라 publish
- Kotlin 1.x 개발이 종료되면 v1 HEAD는 마지막 지원 버전(예: `1.9.25`)에서 freeze.

## Maven Central publish 전제 조건

- 비-SNAPSHOT 버전 publish 시 GPG 서명 필수 (`signingInMemoryKey` 또는 `useGpgCmd()`).
- GitHub Actions 시크릿:
  - `MAVEN_CENTRAL_USERNAME`, `MAVEN_CENTRAL_PASSWORD` — Sonatype OSSRH 계정
  - `SIGNING_IN_MEMORY_KEY`, `SIGNING_IN_MEMORY_KEY_PASSWORD` — GPG 키
- `publish.yml` 현재 구현은 태그(`v*`) push 시 `./gradlew publish` 실행 → Sonatype Central Portal 자동 배포 API 호출 → GitHub Release 생성.

## 참고

- 모델: [kotlinx.serialization의 Kotlin 버전별 release 브랜치 패턴](https://github.com/Kotlin/kotlinx.serialization/branches)
- 이유: 컴파일러 플러그인은 Kotlin 컴파일러 내부 API에 바인딩되므로 Kotlin 패치 단위로 별도 아티팩트가 필요하다.
