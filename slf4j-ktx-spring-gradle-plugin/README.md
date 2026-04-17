# slf4j-ktx-spring-gradle-plugin

**Spring 통합 Gradle 플러그인** — 메인 플러그인을 자동 적용하고 Spring 스테레오타입 어노테이션(`@Component`, `@Controller`, `@Service`, `@Repository`, `@RestController`, `@ControllerAdvice`) FQN을 트리거 목록에 **하드코드로 추가**한다.

## 존재 이유

메인 플러그인이 "Spring을 알지 않는다"는 것이 설계 원칙이다. Spring 사용자 편의를 위해 `@Service` 등을 기본 트리거에 포함시키고 싶지만, 이걸 메인 플러그인에 박으면 비-Spring 프로젝트가 Spring 어노테이션 FQN을 설정 리스트에 달고 다녀야 한다. 관심사 분리 위반.

**kotlin-spring이 kotlin-allopen에게 하는 것과 동일한 패턴**: 별도 플러그인이 메인 플러그인을 `target.plugins.apply(…)`로 자동 적용하고, Spring 특화 어노테이션 목록을 `SubpluginOption`으로 contribute한다.

사용자는 `id("io.github.harryjhin.slf4j-ktx.spring")` 한 줄만 적으면 된다 — 메인 플러그인은 자동으로 걸린다.

## 주요 타입 (Step 11에서 구현)

- `Slf4jKtxSpringGradleSubplugin` — `KotlinCompilerPluginSupportPlugin`
  - `apply(target)` → `target.plugins.apply(Slf4jKtxGradleSubplugin::class.java)` (메인 자동 적용)
  - `getCompilerPluginId()` → 메인 플러그인 id 반환 (동일 컴파일러 플러그인을 재사용)
  - `getPluginArtifact()` → 메인 artifact 반환 (embeddable JAR)
  - `applyToCompilation()` → `SPRING_ANNOTATIONS` 6개를 `SubpluginOption("annotation", fqn)`로 매핑
- `SPRING_ANNOTATIONS` — `Component`, `Controller`, `Service`, `Repository`, `RestController`, `ControllerAdvice` FQN 리스트

## 의존

- `compileOnly`: `kotlin-gradle-plugin-api`, `kotlin-gradle-plugin`
- `implementation(project(":slf4j-ktx-gradle-plugin"))` — 메인 플러그인 클래스 참조용

## 참조

- SPEC §4.1, §5.10, §6.8
- 대응: `kotlin-spring` Gradle 플러그인 (kotlinlang.org/docs/all-open-plugin.html 에 동작 문서화). sparse checkout 외부라 파일 직접 확인은 PLAN Step 1에서 생략, 동작은 문서 기반
