# backend

Spring Boot 4.2 / Kotlin 2.4 (JDK 25) / Gradle(Kotlin DSL) / PostgreSQL / Flyway

자바 버전과 다른 점은 [docs/kotlin-vs-java.md](../docs/kotlin-vs-java.md).

## 패키지 구조

기능(feature)별로 패키지를 나누고, 기능 안에서 헥사고날 구조를 따른다.

```
com.example.template
├── item/                       # 기능 하나
│   ├── domain/                 # 도메인 모델, 규칙 (Spring/JPA 의존 X)
│   ├── application/
│   │   ├── port/in/            # 유스케이스 인터페이스
│   │   ├── port/out/           # 외부 의존 포트 (저장소 등)
│   │   └── service/            # 유스케이스 구현, 트랜잭션
│   └── adapter/
│       ├── in/web/             # 컨트롤러, 요청/응답 DTO
│       └── out/persistence/    # JPA 엔티티, 리포지토리, 포트 구현
└── common/                     # 기능 공통 (예외, 전역 예외 처리, 설정)
```

규칙은 `HexagonalArchitectureTest`(ArchUnit)가 빌드 때 검사한다.

- 기능 안에서는 `adapter -> application -> domain` 방향만 허용
- 컨트롤러는 `port.in`만 사용 (서비스, `port.out` 직접 참조 X)
- 다른 기능은 그 기능의 `application.port.in`과 `domain`으로만 접근
- `common`은 특정 기능을 참조하지 않음

## 실행 / 빌드

```bash
./gradlew bootRun       # compose.yaml의 PostgreSQL 자동 기동 (Docker 필요)
./gradlew bootTestRun   # Testcontainers로 DB 띄워서 실행
./gradlew build         # 테스트 + ArchUnit + ktlint 포맷 검사
./gradlew spotlessApply # ktlint 포맷 자동 수정 (규칙은 루트 .editorconfig)
```

빌드에는 Docker가 필요하다. jOOQ 코드를 만들 때 임시 PostgreSQL 컨테이너를 쓰기 때문이다([JPA와 jOOQ](#jpa와-jooq)).
jOOQ 코드가 이미 생성돼 있으면 Docker가 꺼져 있어도 빌드되고, DB가 필요한 테스트만 skip된다. CI에서는 항상 돈다.

## DB 설정

로컬에서는 `application.yml`에 datasource 설정이 없다.
`spring-boot-docker-compose`가 `compose.yaml`의 PostgreSQL을 띄우고 접속 정보를 자동으로 넣어 준다.
이 의존성은 `developmentOnly`라서 빌드된 jar에는 포함되지 않는다.

로컬에서 기본값을 바꾸려면 `backend/.env`를 만든다 (`.env.example` 참고). 없으면 무시한다.

운영(jar 실행)에서는 환경변수로 넣는다. 전체 목록은 [docs/configuration.md](../docs/configuration.md).

```bash
SPRING_DATASOURCE_URL=jdbc:postgresql://<host>:5432/<db>
SPRING_DATASOURCE_USERNAME=<user>
SPRING_DATASOURCE_PASSWORD=<password>
```

스키마는 Flyway(`src/main/resources/db/migration`)로만 바꾼다. `ddl-auto`는 `validate`.

## 도메인 모델과 JPA 엔티티

도메인 모델(`Item`)과 JPA 엔티티(`ItemJpaEntity`)를 나눈다. 도메인은 JPA를 모르고, 변환은 영속성 어댑터에서만 한다.

수정은 이렇게 흐른다 (`ItemService.update`, `ItemPersistenceAdapter.update`).

1. 서비스(트랜잭션 안)가 포트로 도메인 모델을 조회한다
2. 도메인 메서드(`Item.update`)로 바꾼다. 검증은 도메인이 한다
3. `UpdateItemPort.update`로 넘기면 어댑터가 영속 상태의 엔티티에 값을 옮긴다
4. `save()` 없이 커밋 때 더티 체킹으로 UPDATE 된다. 엔티티는 1차 캐시에 있어서 SELECT가 다시 나가지 않는다

도메인 객체를 바꾸는 것만으로는 저장되지 않는다. 엔티티로 옮기는 3번이 꼭 필요하다.

### 동시 수정

샘플에는 동시 수정 충돌 처리가 없다. 두 사람이 같은 항목을 고치면 나중에 저장한 쪽이 이긴다.
여러 사람이 같은 데이터를 함께 편집하는 기능(문서, 재고, 결재 등)이면 낙관적 락을 넣는다.

1. 테이블에 `version` 컬럼(마이그레이션), 엔티티에 `@Version`
2. 도메인 모델, 응답 DTO, 수정 요청에 `version`을 싣는다 (수정 화면이 받은 값을 그대로 돌려보낸다)
3. 어댑터에서 요청의 version과 엔티티의 version이 다르면 충돌 예외를 던지고, 전역 핸들러에서 409로 바꾼다

엔티티에 `@Version`만 붙이면 같은 순간에 겹친 저장만 막고, "화면을 열어 둔 사이 다른 사람이 저장한" 경우는 막지 못한다.

## 목록 페이징

- 목록(`GET /api/items`)은 커서 페이징이다. `WHERE id < 커서 ORDER BY id DESC LIMIT size+1`로 읽어서 OFFSET 없이 PK 인덱스로 바로 찾아간다. 데이터가 많아도 뒤 페이지가 느려지지 않고, 중간에 항목이 추가돼도 중복/누락이 없다.
- 한 개 더 읽어서 다음 페이지가 있는지 판단하므로 count 쿼리가 없다.
- 커서는 클라이언트에 불투명한 문자열(Base64URL)로 준다 (`ItemCursor`). 정렬 기준을 바꿔도 API 형태는 그대로다.
- 전체 개수나 페이지 번호가 필요한 화면(관리자 표 등)은 검색 API처럼 OFFSET 페이징을 쓴다 (`/api/items/search`).

## JPA와 jOOQ

- 저장/수정(도메인 모델 단위)은 JPA, 검색이나 통계 같은 조회는 jOOQ로 한다.
- 둘 다 `adapter/out/persistence`에서만 쓴다. 같은 DataSource와 트랜잭션을 쓰므로 섞어 써도 된다.

jOOQ 코드는 커밋하지 않고 빌드할 때 만든다.

1. `generateJooq` 태스크가 임시 PostgreSQL 컨테이너를 띄우고
2. Flyway 마이그레이션을 적용한 뒤
3. 그 스키마로 `build/generated-sources/jooq`에 코틀린 코드를 생성한다 (`KotlinGenerator`, 패키지 `com.example.template.common.jooq`). NOT NULL 컬럼은 non-null 타입이 된다.

`compileKotlin` 전에 자동으로 돌고, 마이그레이션 파일이 바뀌지 않으면 건너뛴다.
처음 빌드할 때, 마이그레이션을 추가한 직후, `clean` 뒤에는 Docker가 필요하다. 직접 돌릴 때는 `./gradlew generateJooq`.
생성 로직은 `src/jooqCodegen/kotlin/codegen/JooqCodegen.kt`. 테이블 참조는 `com.example.template.common.jooq.tables.references.ITEMS`.

## 모니터링

- 메트릭: 관리 포트(기본 8081)의 `/actuator/prometheus`
- 트레이스: `OTLP_TRACING_ENABLED=true`면 OTLP로 전송
- 로그: 기본은 콘솔 텍스트. `LOG_CONSOLE_FORMAT=logstash`면 콘솔을 JSON으로 (Docker, k8s 수집용), `LOG_FILE`을 주면 JSON 파일로도 남김. 둘 다 traceId 포함

스택 띄우는 법과 변수는 [infra/observability](../infra/observability/README.md).

## 인증

템플릿에는 인증이 없다. 붙일 때는 [docs/authentication.md](../docs/authentication.md)를 따른다.
인증 정보는 adapter.in에서 꺼내 Command에 사용자 id로 넘기고, 서비스와 도메인은 Spring Security를 모른다 (`HexagonalArchitectureTest`가 검사).

## API 문서

- Swagger UI: http://localhost:8080/swagger-ui.html
- 스펙: http://localhost:8080/v3/api-docs (운영에서 끄려면 `API_DOCS_ENABLED=false`)

`openapi/openapi.json`(저장소 루트)은 프론트 타입 생성에 쓰는 API 계약 파일이다.
API를 바꾸면 `./gradlew updateOpenApi`로 갱신해서 같이 커밋한다. 갱신하지 않으면 `OpenApiSpecTests`가 실패한다.

## Docker

jOOQ 코드 생성에 Docker가 필요해서 jar는 이미지 밖에서 만든다.

```bash
./gradlew bootJar
docker build -t backend .
docker run -p 8080:8080 \
  -e SPRING_DATASOURCE_URL=jdbc:postgresql://<host>:5432/<db> \
  -e SPRING_DATASOURCE_USERNAME=<user> \
  -e SPRING_DATASOURCE_PASSWORD=<password> \
  backend
```

## 샘플 API

- `POST /api/items` - 생성 (201, Location 헤더)
- `GET /api/items?cursor=&size=20` - 목록 (최신순, 커서 페이징). 응답 `{ items, nextCursor }`, 다음 페이지는 `nextCursor`를 그대로 넘긴다. 마지막 페이지면 `nextCursor: null`
- `GET /api/items/{id}` - 조회 (없으면 404)
- `PUT /api/items/{id}` - 수정 (없으면 404)
- `DELETE /api/items/{id}` - 삭제 (204, 없으면 404)
- `GET /api/items/search?keyword=&page=0&size=20` - 이름 검색 + 페이징 (jOOQ)

오류는 Problem Details(`application/problem+json`)로 응답하고, 입력값 검증 실패 시 `errors`에 필드별 메시지가 들어간다.

```json
{
  "title": "Bad Request",
  "status": 400,
  "detail": "입력값을 확인해 주세요.",
  "errors": [{ "field": "name", "message": "must not be blank" }]
}
```
