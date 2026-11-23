# 설정값

로컬에서 실행할 때는 **넣어야 하는 값이 없다.** 모든 값에 로컬 기본값이 있다.
운영처럼 다른 환경에 띄울 때 바꿔야 하는 값만 아래 표의 "운영" 열을 보고 넣는다.

## 어디에 넣나

| 대상 | 로컬 파일 (커밋 안 함) | 예시 파일 | 우선순위 |
|---|---|---|---|
| backend | `backend/.env` | `backend/.env.example` | OS 환경변수 > `backend/.env` > `application.yml` 기본값 |
| frontend | `frontend/.env.local` | `frontend/.env.example` | OS 환경변수 > `.env.local` |
| 루트 `compose.yaml` | `.env` (루트) | `.env.example` | 셸 환경변수 > `.env` |
| 모니터링 스택 | `infra/observability/.env` | `infra/observability/.env.example` | 셸 환경변수 > `.env` |

- `.env` 파일은 모두 `.gitignore`에 들어 있다. 비밀번호, 키는 예시 파일에 넣지 않는다.
- Docker, k8s 같은 배포 환경에서는 파일 대신 **환경변수**(compose `environment`, k8s ConfigMap/Secret)로 넣는다.

### backend/.env 주의

Spring이 `backend/.env`를 properties 파일로 읽는다 (`application.yml`의 `spring.config.import`).

- 아래 표의 대문자 변수(`MANAGEMENT_PORT` 등)는 그대로 쓴다.
- Spring 표준 설정은 **점 표기**로 쓴다. 예: `spring.datasource.url=...`
  `SPRING_DATASOURCE_URL` 같은 대문자 형식은 OS 환경변수에서만 동작하고 `.env` 파일에서는 동작하지 않는다.
- 작업 디렉터리가 `backend/`일 때 읽는다. `./gradlew bootRun`, IntelliJ 실행 설정(`.run/`)은 모두 `backend/`에서 돈다.
- Docker 이미지 안에는 `.env`가 없으니 영향이 없다.

## backend

| 변수 | 기본값 | 로컬 | 운영 | 설명 |
|---|---|---|---|---|
| `SPRING_DATASOURCE_URL` | (없음) | 불필요 | **필수** | 예: `jdbc:postgresql://db:5432/app`. 로컬은 `compose.yaml`의 PostgreSQL이 자동 연결 |
| `SPRING_DATASOURCE_USERNAME` | (없음) | 불필요 | **필수** | |
| `SPRING_DATASOURCE_PASSWORD` | (없음) | 불필요 | **필수 (비밀)** | Secret으로 넣는다 |
| `SERVER_PORT` | 8080 | - | 선택 | API 포트 |
| `MANAGEMENT_PORT` | 8081 | - | 선택 | Actuator(health, prometheus). 외부에 열지 않는다 |
| `API_DOCS_ENABLED` | true | - | **false 권장** | Swagger UI, `/v3/api-docs` |
| `APP_ENV` | local | - | 권장 | 트레이스의 `deployment.environment` (예: dev, prod) |
| `OTLP_TRACING_ENABLED` | false | 모니터링 쓸 때 true | 수집기가 있으면 true | 트레이스 전송 여부 |
| `OTLP_TRACING_ENDPOINT` | http://localhost:4318/v1/traces | - | 수집기 주소 | OTLP HTTP |
| `TRACING_SAMPLING_PROBABILITY` | 1.0 | - | 선택 (예: 0.1) | 트레이스를 만들 요청 비율 |
| `LOG_CONSOLE_FORMAT` | (없음) | - | 수집기가 컨테이너 로그를 읽으면 `logstash` | 콘솔 로그 형식. 비우면 텍스트, `logstash`/`ecs`면 한 줄 JSON (traceId 포함) |
| `LOG_FILE` | (없음) | 모니터링 쓸 때 `logs/backend.log` | 보통 불필요 | 지정하면 JSON 로그 파일. 앱이 호스트에서 돌 때 Alloy가 파일을 읽는 방식 |
| `JAVA_TOOL_OPTIONS` | (없음) | - | 선택 | JVM 옵션. 이미지에는 `-XX:MaxRAMPercentage=75`가 들어 있다 |

헬스체크: `http://<host>:<MANAGEMENT_PORT>/actuator/health` (liveness, readiness 그룹 포함)

## frontend

| 변수 | 기본값 | 로컬 | 운영 | 설명 |
|---|---|---|---|---|
| `BACKEND_URL` | http://localhost:8080 | 백엔드 포트가 다를 때만 | **필수** | 서버에서만 쓴다 (브라우저로 노출 안 됨). http/https 형식이어야 기동된다. 요청 시점에 읽으므로 이미지를 다시 빌드할 필요 없음 |
| `PORT` | 3000 | - | 선택 | Docker 이미지의 Next.js 서버 포트 |

새 변수는 `frontend/src/lib/env.ts`의 스키마에 추가한다. 브라우저에서 써야 하는 값만 `NEXT_PUBLIC_` 접두사를 붙이고, 이 값은 빌드할 때 고정된다.

## 루트 compose.yaml (전체 스택 로컬 확인, E2E)

| 변수 | 기본값 | 설명 |
|---|---|---|
| `BACKEND_PORT` | 8080 | 호스트에 여는 백엔드 포트 |
| `FRONTEND_PORT` | 3000 | 호스트에 여는 프론트 포트 |
| `POSTGRES_DB` | app | 이 스택 안 PostgreSQL |
| `POSTGRES_USER` | app | |
| `POSTGRES_PASSWORD` | app | 로컬 전용 |

## 모니터링 스택 (infra/observability)

변수와 사용법은 [infra/observability/README.md](../infra/observability/README.md) 참고.
앱 쪽에서 켤 것: `OTLP_TRACING_ENABLED=true`, `LOG_FILE=logs/backend.log`.

## 환경별 요약

| | backend | frontend |
|---|---|---|
| 로컬 (`bootRun`, `npm run dev`) | 없음 | 없음 |
| 루트 compose | compose가 넣어 줌 (`.env`로 포트, DB 계정만 변경) | compose가 넣어 줌 |
| 운영 | `SPRING_DATASOURCE_*`, `API_DOCS_ENABLED=false`, `APP_ENV` (+ 모니터링 쓰면 `OTLP_*`, `LOG_CONSOLE_FORMAT=logstash`) | `BACKEND_URL` |

### 로그는 어떻게 남나

- 앱은 항상 콘솔(표준출력)에 로그를 쓴다. Docker는 이걸 그대로 모으므로 `docker compose logs backend`로 볼 수 있다.
- 로그 수집기(Loki 등)로 보낼 때는 콘솔을 JSON으로 바꾼다: `LOG_CONSOLE_FORMAT=logstash`. Docker, k8s 모두 이 방식이 일반적이다.
- `LOG_FILE`은 앱을 호스트에서 직접 띄우고 `infra/observability`의 Alloy가 파일을 읽는 로컬 구성용이다.
