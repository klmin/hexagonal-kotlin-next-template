---
name: add-backend-usecase
description: 백엔드에 새 기능(유스케이스)을 기능별 패키지 + 헥사고날 구조로 추가한다. 새 API, 새 도메인, 기존 기능에 동작을 추가할 때 사용.
---

# 백엔드 기능 추가

`backend/src/main/kotlin/com/example/template/item/`이 참고 구현이다. 코틀린 규칙은 `AGENTS.md`의 "코틀린"과 `docs/kotlin-vs-java.md`. 규칙은 루트 `AGENTS.md`의 "백엔드 구조" 참고.

## 1. 정리
- 입력, 출력, 도메인 규칙, 오류 상황(400/404/409 등)을 먼저 정리한다.
- 기존 기능 패키지에 넣을지, 새 기능 패키지(`<feature>/`)를 만들지 정한다.
- 다른 기능의 데이터가 필요하면 그 기능의 `application.port.in` 유스케이스를 호출한다. 내부 클래스를 직접 쓰지 않는다.

## 2. domain (`<feature>/domain/`)
- 생성은 정적 팩토리(`create`, `restore`), 불변식은 생성 시점에 검사.
- 규칙 위반은 `DomainValidationException`.
- Spring, JPA, Jakarta Validation import 금지.
- 테스트: 순수 단위 테스트.

## 3. application (`<feature>/application/`)
- `port/in/<동사><대상>UseCase` 인터페이스, 입력이 여러 개면 `...Command` data class. 패키지는 `port.`in``(백틱).
- `port/out/`에 필요한 포트 (`Load...Port`, `Save...Port`처럼 작게).
- `service/`에서 구현. 클래스는 `@Transactional(readOnly = true)`, 쓰기 메서드만 `@Transactional`.
- 없는 리소스는 `NotFoundException`을 상속한 예외를 `<feature>/application/`에 만든다 (전역 핸들러가 404로 변환).
- 현재 시각은 `Clock` 주입.
- 테스트: `port.out`을 인메모리 구현으로 바꾼 단위 테스트 (`ItemServiceTest` 참고).

## 4. adapter.out.persistence (`<feature>/adapter/out/persistence/`)
- `...JpaEntity`, `...JpaRepository`, `...PersistenceAdapter`(포트 구현). 전부 `internal`. 엔티티는 일반 class, 프로퍼티는 `protected set`.
- 엔티티와 도메인 변환은 어댑터 안에서만.
- 수정: 어댑터가 `findById`로 영속 엔티티를 얻고 값을 옮긴다. `save()`는 부르지 않는다(더티 체킹). 테스트에서는 `TestEntityManager`로 flush/clear 후 다시 읽어 확인한다.
- 삭제: 없으면 false를 돌려주고 서비스가 NotFound 예외로 바꾼다.
- 검색, 통계처럼 조회가 복잡하면 jOOQ 어댑터로 따로 만든다 (`ItemSearchAdapter` 참고, 테스트는 `@JooqTest`). 생성 코드는 마이그레이션 추가 후 빌드하면 갱신된다.
- `db/migration/V{다음번호}__설명.sql` 추가. 기존 파일은 수정하지 않는다.
- 테스트: `@DataJpaTest` + `@AutoConfigureTestDatabase(replace = NONE)` + `@Import({TestcontainersConfiguration.class, 어댑터.class})` + `@Testcontainers(disabledWithoutDocker = true)`.

## 5. adapter.in.web (`<feature>/adapter/in/web/`)
- 컨트롤러, `...Request`(Bean Validation), `...Response`(data class, `companion object`의 `from(도메인)`). 요청 DTO는 `String?` + `@field:NotBlank`.
- 컨트롤러는 `port.in`에만 의존.
- 생성은 `201 Created` + `Location`.
- 테스트: `@WebMvcTest(컨트롤러::class)` + `@MockkBean`, MockMvc 코틀린 DSL. 성공, 검증 실패(400, `errors`), 없음(404) 확인.

## 6. 검증
```bash
cd backend && ./gradlew build
```
`HexagonalArchitectureTest`가 실패하면 의존 방향을 고친다. 테스트를 바꿔서 통과시키지 않는다.

## 7. 마무리
- API를 추가하거나 바꿨으면 `./gradlew updateOpenApi` 후 `frontend`에서 `npm run api:types`. 두 생성 파일을 같이 커밋한다.
- `backend/README.md`의 샘플 API 목록을 갱신한다.
- 화면이 필요하면 `add-frontend-feature`로 이어서 진행.
