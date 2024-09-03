# Kotlin 사용자를 위한 SLF4J 확장 라이브러리

`slf4j-api-extensions`는 Kotlin 사용자들에게 SLF4J(Simple Logging Facade for Java)를 더욱 편리하게 사용할 수 있도록 돕는 라이브러리입니다.

## 주요 특징

1. `slf4j-api` 확장: 구현체를 직접 사용하는 대신 `slf4j-api`를 확장하여 사용할 수 있는 기능을 제공합니다.
   즉, `slf4j-simple`, `log4j2`, `logback` 등의 구현체를 사용하는 경우에 모두 동일한 API를 사용할 수 있습니다.
2. 캐싱: `Logger` 인스턴스를 캐싱하여 불필요한 인스턴스 생성을 줄여줍니다.
3. 함수형 패러다임: 함수형 프로그래밍 스타일로 로깅을 할 수 있도록 지원합니다.

## dependency

```kotlin
dependencies {
    implementation("io.github.debop:slf4j-api-extensions:$version")
}
```

## 구성

### Logger Cache

`Logger` 인스턴스 생성은 비용이 비싸기 때문에, `Logger` 인스턴스를 캐싱하여 불필요한 인스턴스 생성을 줄여줍니다.
단, 최대 1,000개의 `Logger` 인스턴스만 캐싱합니다.
만약 캐싱된 `Logger` 인스턴스가 1,000개를 넘어가면, 가장 오래된 `Logger` 인스턴스를 제거합니다.
최대 캐싱 개수를 변경하려면 `slf4j-api-extensions.properties` 파일을 생성하고 `max-cache-size` 값을 변경하면 됩니다.

`src/main/resources/slf4j-api-extensions.properties`:

```properties
max-cache-size=100
```

## 사용법

### trace

```kotlin
class MyClass {

    fun myFunc() {
        trace { "trace message" }
    }
}
```

### debug

```kotlin
class MyClass {

    fun myFunc() {
        debug { "debug message" }
    }
}
```

### info

```kotlin
class MyClass {

    fun myFunc() {
        info { "info message" }
    }
}
```

### warn

```kotlin
class MyClass {

    fun myFunc() {
        warn { "warn message" }
    }
}
```

### error

```kotlin
class MyClass {

    fun myFunc() {
        error { "error message" }
    }
}
```
