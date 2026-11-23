# AGENTS.md

이 저장소의 작업 규칙.

## 구성

- `backend/` - Spring Boot 4.2, Kotlin 2.4 (JDK 25), Gradle(Kotlin DSL), PostgreSQL, Flyway ([README](backend/README.md))
- `docs/kotlin-vs-java.md` - 자바 버전과 다른 점. 코틀린 관련 판단이 필요하면 먼저 본다
- `openapi/openapi.json` - API 계약. 백엔드 `./gradlew updateOpenApi`로 생성, 프론트 `npm run api:types`로 타입 생성
- `docs/configuration.md` - 설정값 목록. 환경변수를 추가하거나 바꾸면 이 문서와 해당 `.env.example`도 같이 고친다
- `docs/authentication.md` - 인증을 붙일 때 무엇을 어디에 두는지 (템플릿에는 인증 코드 없음)
- `infra/observability/` - 로컬 모니터링 스택 ([README](infra/observability/README.md))
- `frontend/` - Next.js 16 (App Router, Cache Components), React 19, TypeScript, Tailwind 4, shadcn/ui ([README](frontend/README.md), [AGENTS](frontend/AGENTS.md))

## 명령

```bash
# backend (JDK 25)
cd backend && ./gradlew build

# frontend (Node 22.22.2+, 24.15+ 또는 26+. CI와 Docker 이미지는 26 LTS)
cd frontend && npm ci
npm run format:check && npm run lint && npm run typecheck && npm test && npm run build
```

작업을 끝내기 전에 바뀐 쪽의 명령을 돌려서 통과시킨다.
백엔드 빌드는 jOOQ 코드 생성(`generateJooq`)에 Docker가 필요하다. 처음 빌드할 때, 마이그레이션을 바꾼 뒤, `clean` 뒤에는 Docker 없이 빌드할 수 없다.
코드가 이미 생성돼 있으면 Docker 없이도 빌드되고, Testcontainers 테스트만 skip되어 CI에서만 돈다.

## 백엔드 구조 (기능별 패키지 + 헥사고날)

패키지 루트는 `com.example.template`. 기능마다 패키지를 만들고 그 안을 아래처럼 나눈다.

```
<feature>/domain/                    도메인 모델, 규칙. Spring/JPA/Validation 의존 금지
<feature>/application/port/in/       유스케이스 인터페이스 (+ Command/Query data class)
<feature>/application/port/out/      외부 의존 포트 인터페이스
<feature>/application/service/       유스케이스 구현. @Transactional은 여기
<feature>/application/               기능 예외 (예: ItemNotFoundException)
<feature>/adapter/in/web/            컨트롤러, Request/Response DTO
<feature>/adapter/out/persistence/   JPA 엔티티/리포지토리, jOOQ 조회, 포트 구현
common/error/                        공통 예외 (DomainValidationException, NotFoundException)
common/web/                          전역 예외 처리 (Problem Details)
common/config/                       공통 Bean
common/jooq/                         jOOQ 생성 코드 (빌드 시 생성, 커밋 X)
```

`HexagonalArchitectureTest`가 검사하는 규칙:

- 기능 안에서는 `adapter -> application -> domain` 방향만
- 컨트롤러(adapter.in)는 `port.in`만 사용. 서비스나 `port.out`을 직접 쓰지 않는다
- adapter.in과 adapter.out은 서로 참조하지 않는다
- 다른 기능은 그 기능의 `application.port.in`과 `domain`으로만 접근. 기능 간 순환 참조 금지
- `common`은 특정 기능을 참조하지 않는다
- jOOQ(`org.jooq`, `common.jooq`)는 adapter.out에서만 사용
- Spring Security는 application, domain에서 사용 금지. 인증 정보는 adapter.in에서 꺼내 Command에 사용자 id로 넘긴다
- adapter.out의 최상위 클래스(JPA 엔티티, 리포지토리, 어댑터)는 `internal`(또는 `private`). 엔티티를 어댑터 밖으로 내보내지 않고 도메인 모델로 변환한다
- `@Transactional`(클래스, 메서드)은 `application.service`에만
- 현재 시각은 `Clock` Bean 주입. `Instant.now()`, `LocalDateTime.now()`, `YearMonth.now()` 같은 java.time 타입의 직접 호출, 코틀린 `Clock.System.now()`, `System.currentTimeMillis()` 금지(`now(clock)`, `clock.millis()`는 허용), `Clock.systemUTC()` 등은 `common.config`에서만

그 외:

- 수정은 서비스에서 조회 -> 도메인 메서드로 변경 -> `Update...Port`로 넘기고, 어댑터가 영속 엔티티에 값을 옮겨 더티 체킹으로 반영한다 (`ItemPersistenceAdapter.update` 참고)
- 도메인 규칙 위반은 `DomainValidationException`(400), 없는 리소스는 `NotFoundException`을 상속한 기능 예외(404)
- 목록 API는 커서 페이징이 기본이다 (`ListItemsQuery`, `ItemCursor` 참고). 전체 목록을 한 번에 돌려주는 API를 만들지 않는다
- 저장/수정은 JPA, 검색/통계 같은 조회는 jOOQ. jOOQ 코드는 Flyway 마이그레이션으로 빌드 때 생성되니 직접 고치지 않는다
- 스키마 변경은 Flyway `V{n}__설명.sql`로만. 머지된 마이그레이션은 고치지 않는다
- 로그는 SLF4J로 남긴다. traceId는 자동으로 붙으니 직접 넣지 않는다. 개인정보, 비밀값은 로그에 남기지 않는다

### 코틀린 (자세한 이유는 docs/kotlin-vs-java.md)

- `in`은 예약어라 `port.`in``, `adapter.`in`` 처럼 백틱으로 쓴다
- Command/Query/Result/Request/Response는 `data class`. 도메인 모델과 JPA 엔티티는 일반 class (data class의 `copy`, equals 문제)
- 생성 시 검증은 `init { }`. 상수는 `companion object`의 `const val`
- 요청 DTO의 필수 값도 `String?` + `@field:NotBlank`로 받는다 (빠진 값을 Bean Validation 오류로 응답하려고). 검증 어노테이션은 `@field:`를 붙인다
- "없을 수 있음"은 `Optional` 대신 nullable 타입(`Item?`). Spring Data는 `findByIdOrNull`
- `!!`는 검증을 통과했거나 저장된 값처럼 null이 아님이 확실한 곳에서만 쓴다
- 응답의 nullable 필드는 `@field:Schema(types = ["string", "null"])`처럼 직접 적는다 (springdoc 의 코틀린 nullable 자동 표시를 꺼 둠)

### 테스트

- domain: 순수 단위 테스트
- application.service: `port.out`을 인메모리 구현으로 바꿔서 단위 테스트
- adapter.in.web: `@WebMvcTest` + `@MockkBean`(springmockk)으로 유스케이스 mock. MockMvc 코틀린 DSL(`mockMvc.post(...) { }.andExpect { }`)
- adapter.out.persistence: JPA는 `@DataJpaTest`, jOOQ는 `@JooqTest`. 둘 다 Testcontainers PostgreSQL + `@Testcontainers(disabledWithoutDocker = true)`

## 프론트엔드

- Next.js 16은 이전 버전과 API가 다르다. 코드 쓰기 전에 `frontend/node_modules/next/dist/docs/`에서 해당 문서를 확인한다
- 백엔드 호출은 서버에서만. `src/lib/api/client.ts`의 `request()`를 쓰고 기능별 파일(`items.ts` 등)에 함수를 둔다. `import "server-only"` 필수
- 서버 환경변수는 `src/lib/env.ts`(zod)에 추가하고 `serverEnv()`로 읽는다
- 캐시하지 않는 데이터를 읽는 서버 컴포넌트는 `<Suspense>` 안에 둔다 (아니면 `next build` 실패)
- UI는 `src/components/ui/`의 shadcn/ui 컴포넌트를 쓴다. 추가는 `npx shadcn@latest add <이름>`. 색은 테마 토큰(`text-muted-foreground` 등)
- 변경은 Server Action + `useActionState`. 성공하면 `refresh()`(`next/cache`). 예상 가능한 오류는 throw 말고 상태로 반환
- `next lint`는 없다. `npm run lint` 사용. 포맷은 Prettier(`npm run format`)
- 테스트는 Vitest(`npm test`), 대상 파일 옆에 `*.test.ts(x)`. async 서버 컴포넌트와 화면 흐름은 Playwright(`e2e/`)로 확인
- E2E는 루트 `compose.yaml`로 전체 스택을 띄우고 `npm run test:e2e`. 데이터가 쌓이므로 테스트마다 고유한 값을 쓴다
- 백엔드 API 타입은 `src/lib/api/schema.d.ts`(생성 파일)에서 가져온다. 직접 고치지 않는다

## API를 바꿀 때

1. 백엔드 수정 후 `cd backend && ./gradlew updateOpenApi`
2. `cd frontend && npm run api:types`
3. `openapi/openapi.json`, `frontend/src/lib/api/schema.d.ts`를 코드와 같이 커밋

## Git / PR

- 브랜치: `feat/`, `fix/`, `refactor/`, `chore/`, `docs/`, `test/`
- 커밋: Conventional Commits, 설명은 한국어. 예) `feat(backend): 주문 취소 API 추가`
- PR 하나에 목적 하나. 커밋은 논리 단위로 나눈다

## Review guidelines

PR 리뷰 시 아래를 우선 본다. 스타일, 취향 문제는 지적하지 않거나 낮은 우선순위로.

- 위 헥사고날 규칙 위반 (domain의 프레임워크 의존, 컨트롤러의 서비스/리포지토리 직접 참조, 엔티티 노출, 기능 간 내부 참조)
- 트랜잭션 경계 누락이나 잘못된 위치
- 머지된 Flyway 마이그레이션 수정, 엔티티와 스키마 불일치
- 입력 검증 누락, Problem Details가 아닌 오류 응답
- 프론트에서 백엔드를 브라우저로 직접 호출하거나 서버 전용 값을 `NEXT_PUBLIC_`으로 노출
- 새 기능에 테스트 없음
- 비밀값(키, 비밀번호, 토큰) 커밋
