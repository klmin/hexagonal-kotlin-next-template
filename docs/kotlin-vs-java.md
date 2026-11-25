# 자바 버전과 다른 점

이 저장소는 [hexagonal-java-react-template](https://github.com/klmin/hexagonal-java-react-template)의 백엔드를 코틀린으로 옮긴 것이다.
패키지 구조, API(`openapi/openapi.json`), 프론트, 인프라, ArchUnit 규칙은 같다. 같은 E2E 테스트가 두 백엔드에서 모두 통과한다.

두 저장소의 같은 파일을 나란히 열어 보면서 읽는 것을 추천한다. 코드 주석에도 "자바에서는 이렇게, 코틀린에서는 이래서 이렇게"를 적어 두었다.

## 한눈에 보기

| 자바 | 코틀린 | 예시 파일 |
|---|---|---|
| `record` | `data class` | `CreateItemCommand.kt` |
| record compact constructor 검증 | `init { }` 블록 | `ListItemsQuery.kt` |
| `private static final` 상수 | `companion object` 의 `const val` | `Item.kt` |
| `static` 팩토리 메서드 | `companion object` 안의 함수 | `Item.create`, `Item.restore` |
| 유틸 클래스(private 생성자 + static) | `object` | `ItemCursor.kt` |
| `Optional<Item>` | `Item?` + `?:` (엘비스) | `LoadItemPort.kt`, `ItemService.getItem` |
| `repository.findById(id).map(...)` | `repository.findByIdOrNull(id)?.let(...)` | `ItemPersistenceAdapter.kt` |
| package-private | `internal` (모듈 단위라 더 넓다) | `ItemJpaEntity.kt` |
| 생성자 + 필드 대입 | 주 생성자의 `private val` | `ItemService.kt` |
| `new ArchCondition<>() { }` | `object : ArchCondition<...>() { }` | `HexagonalArchitectureTest.kt` |
| try-with-resources | `.use { }` | `JooqCodegen.kt` |
| `@MockitoBean`, `given().willReturn()` | `@MockkBean`, `every { } returns` | `ItemControllerTest.kt` |
| `mockMvc.perform(post(...)).andExpect(status().isOk())` | `mockMvc.post(...) { }.andExpect { status { isOk() } }` | `ItemControllerTest.kt` |
| `"""` 텍스트 블록 | `"""` 원시 문자열 | `ItemControllerTest.kt` |
| jOOQ `JavaGenerator` | `KotlinGenerator` (NOT NULL 컬럼이 non-null 타입) | `JooqCodegen.kt` |

## 실제로 부딪히는 것들

### 1. `in` 은 예약어다

`port.in`, `adapter.in` 패키지를 코틀린에서는 그대로 쓸 수 없다. 백틱으로 감싼다.

```kotlin
package com.example.template.item.application.port.`in`
import com.example.template.item.application.port.`in`.CreateItemUseCase
```

자바 버전과 구조를 똑같이 맞추려고 백틱을 썼다. 새 프로젝트라면 `port.inbound` / `port.outbound`로 이름을 바꾸는 것도 방법이다.
그 경우 `HexagonalArchitectureTest`의 패키지 이름과 `AGENTS.md`도 같이 바꾼다. `out`은 예약어가 아니라 그대로 쓸 수 있다.

### 2. 클래스는 기본이 final 이다

코틀린 클래스는 기본으로 상속할 수 없다. Spring은 `@Transactional`, `@Configuration` 같은 기능을 프록시(하위 클래스)로 만들고, JPA는 지연 로딩 프록시를 만든다.

- `kotlin("plugin.spring")`: `@Component`, `@Service`, `@Transactional`, `@Configuration` 등이 붙은 클래스를 open으로
- `allOpen { annotation("jakarta.persistence.Entity") }`: JPA 엔티티를 open으로 (plugin.spring은 엔티티를 열지 않는다)
- `kotlin("plugin.jpa")`: 엔티티에 인자 없는 생성자를 만든다

설정은 `backend/build.gradle.kts`.

### 3. 어노테이션 대상 (`@field:`)

생성자 프로퍼티(`val name`)는 파라미터, 필드, getter가 한 번에 생기는 자리라 어노테이션이 어디에 붙을지 정해야 한다.
예전 코틀린은 **생성자 파라미터에만** 붙여서, Bean Validation(필드를 봄)이 `@NotBlank`를 못 보고 검증이 조용히 빠졌다.
코틀린 2.4(이 저장소)는 기본으로 **파라미터와 필드 모두**에 붙여서 `@NotBlank`만 써도 동작한다(그 전 버전에서는 `-Xannotation-default-target=param-property` 옵션으로 켜야 했다. 2.4에서 이 옵션을 주면 "redundant" 경고가 난다).
그래도 이 저장소는 의도를 드러내고 버전에 상관없이 같게 동작하도록 `@field:`를 명시한다.

```kotlin
data class CreateItemRequest(
    @field:NotBlank @field:Size(max = Item.NAME_MAX_LENGTH) val name: String?,
)
```

`@Schema`도 같은 이유로 `@field:Schema`로 쓴다. 반대로 `@Column`, `@Id`처럼 파라미터에 붙일 수 없는 어노테이션은 알아서 필드로 간다.

### 4. 요청 DTO 는 nullable 로 받는다

`name: String`(non-null)이면 `name`이 빠진 요청을 Jackson이 역직렬화 단계에서 실패시킨다.
그러면 "입력값을 확인해 주세요 + errors[].field" 응답이 아니라 JSON 형식 오류가 된다.
`name: String?` + `@field:NotBlank`로 받아서 Bean Validation이 처리하게 하고, 검증을 통과한 뒤 `toCommand()`에서 `name!!`로 꺼낸다.
`ItemControllerTest.createRejectsMissingName`이 이 동작을 확인한다.

### 5. Spring 의 `@Nullable` 이 타입이 된다

Spring 7은 JSpecify(`@Nullable`)로 null 가능 여부를 표시하고, 코틀린 2.x는 이를 기본으로 엄격하게 반영한다. 그래서 Spring이 nullable로 표시한 값은 코틀린에서 `?` 타입이다.
(`-Xjsr305=strict`는 아직 JSR-305 어노테이션을 쓰는 다른 라이브러리까지 같은 방식으로 맞춘다)

- `ObservationConfig`: `context.carrier`가 `HttpServletRequest?`라서 `context.carrier?.requestURI`로 써야 컴파일된다. 자바는 경고 없이 통과한다
- `GlobalExceptionHandler`: 부모 메서드 반환 타입이 `ResponseEntity<Any>?`

자바에서 런타임 NPE가 될 수 있던 곳을 컴파일러가 미리 알려 주는 셈이다.

### 6. null 을 타입으로 막는다

`Item.restore(id: Long, ...)`는 `id`가 non-null 타입이라 `null`을 넘기는 코드가 컴파일되지 않는다.
그래서 자바 버전의 `restoreRequiresId` 테스트는 코틀린 버전에 없다.

`Item.id`는 `Long?`이다(저장 전에는 없음). 저장된 Item만 다루는 곳(응답 변환, 어댑터)에서는 `item.id!!`로 꺼낸다.

### 7. data class 를 쓰지 않는 곳

- **도메인 모델(`Item`)**: data class는 `copy()`가 생겨서 생성자 검증을 거치지 않은 객체를 만들 수 있다. 일반 class + private 생성자 + `create`/`restore`
- **JPA 엔티티(`ItemJpaEntity`)**: equals/hashCode가 모든 필드를 비교해서 지연 로딩, 영속성 컨텍스트와 맞지 않는다
- **값을 정리해서 저장해야 하는 Query(`SearchItemsQuery`)**: data class 생성자 프로퍼티는 바꿀 수 없다. private 생성자 + `companion object`의 `operator fun invoke`로 정리한 값을 넘긴다. `@ConsistentCopyVisibility`로 `copy()`도 private으로 막는다

### 8. 엔티티의 setter 막기

```kotlin
@Column(nullable = false, length = 100)
var name: String = name
    protected set
```

밖에서는 읽기만 하고 값은 `update()`로만 바꾼다. 엔티티가 open 클래스라 `private set`은 쓸 수 없어서 `protected set`이다.

### 9. springdoc 이 nullable 을 스펙에 넣는다

springdoc은 `String?`을 보면 스펙에 `"type": ["string", "null"]`로 적는다. 요청 DTO는 nullable로 받으므로(4번) 자바 버전과 스펙이 달라진다.
`application.yml`에서 `springdoc.model-converters.kotlin-nullable-property-customizer.enabled: false`로 끄고,
응답의 nullable 필드는 자바 버전처럼 `@field:Schema(types = ["string", "null"])`로 직접 적었다. 그래서 `openapi.json`(=프론트 타입)이 자바 버전과 같다.

### 10. ArchUnit 과 companion object

ArchUnit은 바이트코드를 보므로 규칙은 그대로 동작한다. 다만

- `@ArchTest` 필드는 static이어야 해서 `companion object` 안에 `@JvmField`로 둔다
- `companion object`는 `ListItemsQuery$Companion` 같은 중첩 클래스가 된다. "port 안의 클래스는 인터페이스" 규칙에 걸리지 않게 `areTopLevelClasses()`를 추가했다
- `internal`은 바이트코드에서 public이라, package-private처럼 "어댑터 밖에서 엔티티를 못 쓰게" 컴파일러가 막아 주지는 않는다(같은 모듈 안에서는 보인다).
  그래서 자바 버전의 `notBePublic()` 규칙은 코틀린에서 모든 클래스를 통과시킨다. `outboundAdaptersAreInternal`은 바이트코드 대신
  `kotlin-reflect`로 코틀린 메타데이터의 가시성(`KVisibility.INTERNAL`)을 읽어서 검사한다. 최상위 함수만 담은 `XxxKt` 파일 클래스는 건너뛴다
- `@Transactional` 위치, `Instant.now()` 금지 규칙은 바이트코드의 어노테이션, 메서드 호출을 보므로 자바 버전 그대로 동작한다

### 11. MockK 는 기본이 엄격하다

stub 하지 않은 메서드를 부르면 예외가 난다. 반환값이 없는 메서드도 `justRun { useCase.delete(1L) }`처럼 허용해 둬야 한다.
Mockito는 기본값(null, 0)을 돌려준다.

### 12. 테스트에서 internal 타입

`internal class ItemPersistenceAdapter`를 테스트 클래스의 public 프로퍼티로 두면 "internal 타입 노출" 컴파일 오류가 난다.
`@Autowired private lateinit var adapter: ItemPersistenceAdapter`처럼 private으로 둔다.

### 13. checked 예외와 트랜잭션 롤백

Spring `@Transactional`은 기본으로 RuntimeException/Error에서만 롤백하고, checked 예외(`IOException` 등)는 **커밋**한다.
자바는 `throws IOException`이 시그니처에 드러나서 알아채기 쉽지만, 코틀린에는 checked 예외가 없어 서비스가 자바 API를 부르다 `IOException`이 나도 아무 표시가 없다.
그러면 앞에서 저장한 내용이 반쯤 커밋된다.

`common/config/TransactionConfig.kt`에서 `@EnableTransactionManagement(rollbackOn = RollbackOn.ALL_EXCEPTIONS)`로 모든 예외에서 롤백하게 했다.
`TransactionConfigTest`가 이 동작을 확인한다(설정을 빼면 실패한다). 메서드마다 `@Transactional(rollbackFor = [Exception::class])`를 붙이는 방법도 있지만 빠뜨리기 쉽다.

### 14. 경고는 오류로

`build.gradle.kts`의 `allWarningsAsErrors = true`로 컴파일 경고가 있으면 빌드가 실패한다. 플랫폼 타입, 불필요한 `!!` 같은 null 관련 경고가 쌓이지 않게 한다.

### 15. 포맷과 린트 (ktlint)

두 버전 모두 Spotless로 포맷을 검사한다(`./gradlew build`에 포함). 자바는 palantir-java-format, 코틀린은 ktlint. 어긋나면 빌드가 실패하고 `./gradlew spotlessApply`로 고친다.

- 스타일은 `intellij_idea` (루트 `.editorconfig`). 기본값 `ktlint_official`은 파라미터 두 개짜리 함수도 여러 줄로 나눠서 IntelliJ의 Reformat Code와 계속 부딪힌다
- `package-name` 규칙은 껐다. 백틱 패키지(`` port.`in` ``)를 허용하지 않기 때문이다 (1번)

## 직접 해 보기

1. `Item`에 규칙을 하나 추가해 보기 (예: 이름에 금지어). 도메인 테스트부터 쓴다
2. 기능 하나를 새로 만들어 보기 (`.claude/skills/add-backend-usecase`의 순서). 자바 버전과 같은 순서로 하되 코틀린 문법으로
3. `port.in` 을 `port.inbound`로 바꿔 보기. ArchUnit 규칙과 문서까지 고쳐서 빌드가 통과하게
4. 일부러 규칙을 어겨 보기: 서비스에서 `ItemJpaRepository`를 주입하면 ArchUnit이 막는지 확인
