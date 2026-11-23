# 인증 붙이기

이 템플릿에는 인증이 없다. 서비스마다 방식(OIDC, 사내 SSO, 세션, API 키)이 달라서 코드는 넣지 않았다.
이 문서는 인증을 붙일 때 **헥사고날 구조에서 무엇을 어디에 두는지**를 정한다. 아래 코드는 예시이고 저장소에 들어 있지 않다.

## 원칙

- 인증(누구인가)과 기술 세부(JWT, 세션, `SecurityContextHolder`)는 **adapter.in과 `common/security`에만** 둔다
- 컨트롤러가 인증 정보에서 사용자 id를 꺼내 **Command/Query에 일반 값으로** 넘긴다
- 서비스와 도메인은 Spring Security를 모른다. "작성자만 수정할 수 있다" 같은 **업무 규칙은 도메인**에 둔다
- 오류 응답은 401, 403도 Problem Details로 한다
- 프론트는 지금처럼 백엔드를 서버에서만 호출한다. 토큰을 브라우저 JavaScript로 내보내지 않는다

`HexagonalArchitectureTest`가 이 중 일부를 검사한다.

| 규칙 | 막는 것 |
|---|---|
| `domainIsFrameworkFree` | 도메인에서 Spring(Security 포함) 사용 |
| `applicationDoesNotUseSecurity` | 서비스, 포트, Command에서 `org.springframework.security..` 사용 |
| `commonDoesNotDependOnFeatures` | `common/security`가 특정 기능을 참조 |

## 어디에 두나

```
common/security/                 SecurityFilterChain, 401/403 Problem Details 처리. 기능을 참조하지 않는다
<feature>/adapter/in/web/        인증 정보에서 사용자 id를 꺼내 Command에 넣는다. URL/역할 단위 접근 제어
<feature>/application/port/in/   Command에 사용자 id(String/long 같은 일반 값)
<feature>/application/service/   Command의 사용자 id를 도메인에 넘긴다. Security API 사용 금지
<feature>/domain/                소유자 확인 같은 업무 규칙. 프레임워크 의존 금지
common/error/                    ForbiddenException(403) 추가, GlobalExceptionHandler에서 매핑. 도메인도 이 예외를 던질 수 있다
```

| 하고 싶은 것 | 위치 |
|---|---|
| 토큰 검증, 로그인 여부 확인 | `common/security` (SecurityFilterChain) |
| "관리자만 이 API 호출" 같은 역할 검사 | adapter.in (SecurityFilterChain의 경로 규칙, 또는 `@EnableMethodSecurity` + 컨트롤러의 `@PreAuthorize`) |
| "작성자만 수정/삭제" 같은 데이터 기준 검사 | domain (조회한 엔티티와 Command의 사용자 id 비교) |
| "관리자는 남의 글도 삭제" 처럼 역할이 업무 규칙에 들어갈 때 | adapter.in에서 역할을 일반 값으로 바꿔 Command에 넣고(예: `boolean admin`), 도메인이 판단 |

## 백엔드 예시 (OIDC 리소스 서버)

Keycloak 같은 OIDC 서버가 발급한 JWT를 검증하는 경우다.

```kotlin
// build.gradle.kts
implementation("org.springframework.boot:spring-boot-starter-security-oauth2-resource-server")
testImplementation("org.springframework.boot:spring-boot-starter-security-test")
```

Spring Boot 4.2에서는 `spring-boot-starter-oauth2-resource-server`가 deprecated다. 위 이름을 쓴다.

```yaml
# application.yml
spring:
  security:
    oauth2:
      resourceserver:
        jwt:
          issuer-uri: ${OIDC_ISSUER_URI}
```

환경변수를 추가하면 `docs/configuration.md`와 `backend/.env.example`도 같이 고친다.

### common/security

```kotlin
import org.springframework.security.config.annotation.web.invoke // Spring Security 코틀린 DSL

@Configuration
class SecurityConfig {

    @Bean
    fun securityFilterChain(http: HttpSecurity): SecurityFilterChain {
        http {
            authorizeHttpRequests {
                authorize("/v3/api-docs/**", permitAll)
                authorize("/swagger-ui/**", permitAll)
                authorize("/swagger-ui.html", permitAll)
                authorize(anyRequest, authenticated)
            }
            oauth2ResourceServer { jwt { } }
            sessionManagement { sessionCreationPolicy = SessionCreationPolicy.STATELESS }
            // 브라우저가 직접 호출하지 않고 Next.js 서버가 토큰으로 호출하므로 CSRF 토큰이 필요 없다
            csrf { disable() }
        }
        return http.build()
    }
}
```

- 기본 401/403 응답은 Problem Details가 아니다. `authenticationEntryPoint`, `accessDeniedHandler`를 설정해서 `GlobalExceptionHandler`와 같은 형식(`application/problem+json`)으로 응답한다
- 관리 포트(8081)는 지금처럼 외부에 열지 않는 것을 전제로 한다. 관리 포트까지 인증을 걸면 Alloy, k8s 프로브가 `/actuator/prometheus`, `/actuator/health`를 못 읽으니 그 경로는 열어 둔다

### adapter.in: 사용자 id를 Command로

```kotlin
// 컨트롤러
@PutMapping("/{id}")
fun update(
    @PathVariable id: Long,
    @Valid @RequestBody request: UpdateItemRequest,
    @AuthenticationPrincipal jwt: Jwt,
): ItemResponse = ItemResponse.from(updateItemUseCase.update(request.toCommand(id, jwt.subject)))

// application.port.`in`: Security 타입이 아니라 일반 값
data class UpdateItemCommand(val id: Long, val name: String, val description: String?, val requesterId: String)
```

`Jwt`, `Authentication`, `@AuthenticationPrincipal`은 컨트롤러에서 끝난다. 여러 컨트롤러에서 같은 변환을 반복하면 adapter.in에 `HandlerMethodArgumentResolver`를 만들어 사용자 id를 바로 받게 해도 된다.

### domain: 업무 규칙

```kotlin
// Item.update 안에서
if (ownerId != requesterId) {
    throw ForbiddenException("이 항목을 수정할 권한이 없습니다.") // common/error, 403
}
```

도메인은 application 패키지를 참조할 수 없으므로, `DomainValidationException`처럼 `common/error`의 예외를 던진다.
서비스는 지금과 같다. 조회 -> 도메인 메서드 -> `UpdateItemPort`. 소유자 컬럼이 필요하면 Flyway 마이그레이션을 새로 추가한다 (머지된 마이그레이션은 고치지 않는다).

## 테스트

- domain, application.service: 사용자 id가 일반 값이라 지금처럼 단위 테스트한다. Security 설정이 필요 없다
- adapter.in.web: `@WebMvcTest` + `spring-security-test`의 `jwt()`로 인증된 요청을 만든다

```kotlin
mockMvc.put("/api/items/1") {
    with(jwt().jwt { it.subject("user-1") })
    contentType = MediaType.APPLICATION_JSON
    content = body
}.andExpect { status { isOk() } }
```

  `SecurityConfig`가 `@WebMvcTest`에 잡히지 않으면 `@Import(SecurityConfig::class)`로 넣는다. 인증 없는 요청이 401(Problem Details)인지도 확인한다
- E2E: OIDC 서버를 compose에 추가하거나 테스트용 사용자로 로그인하는 흐름을 둔다

## 프론트

지금 구조(브라우저 -> Next.js 서버 -> 백엔드)를 그대로 쓴다.

- 로그인은 Next.js 서버에서 처리한다 (Auth.js 같은 라이브러리. Next.js 16 지원 여부를 확인하고 고른다)
- 세션은 httpOnly 쿠키에 두고, access token은 서버에서만 읽는다
- `src/lib/api/client.ts`의 `request()`가 서버에서 `Authorization: Bearer <token>` 헤더를 붙인다. 기능별 API 함수는 바꾸지 않는다
- 토큰이나 OIDC client secret을 `NEXT_PUBLIC_` 환경변수나 클라이언트 컴포넌트 props로 넘기지 않는다
- 백엔드가 401을 주면 로그인 화면으로 보내고, 403은 화면에 오류로 보여 준다 (예상 가능한 오류라 Server Action은 throw하지 않고 상태로 반환)

## 인증이 필요 없는 서비스

내부 API나 배치처럼 사용자가 없으면 아무것도 하지 않는다. 위 ArchUnit 규칙은 Spring Security가 없어도 통과한다.
