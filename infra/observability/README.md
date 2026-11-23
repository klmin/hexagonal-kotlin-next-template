# observability

로컬 모니터링 스택. Alloy가 수집해서 Prometheus(메트릭), Loki(로그), Tempo(트레이스)에 넣고 Grafana로 본다.

```
backend
 ├ /actuator/prometheus (관리 포트 8081)  <- Alloy가 15초마다 가져감 -> Prometheus
 ├ OTLP 트레이스                           -> Alloy :4318 -> Tempo
 └ JSON 로그 파일 (LOG_FILE)               <- Alloy가 읽음 -> Loki
```

## 띄우기

```bash
# 앱 로그 폴더를 먼저 만든다. 없으면 Linux에서 Docker가 root 소유로 만들어 앱이 로그 파일을 쓰지 못한다
mkdir -p backend/logs

cd infra/observability
cp .env.example .env
docker compose up -d
```

- Grafana: http://localhost:3300 (admin / admin), 대시보드 App > App 개요
- Alloy 상태: http://localhost:12345
- Prometheus: http://localhost:9090

다른 스택과 포트가 겹치면 `.env`에서 포트를 바꾼다.

## 앱 설정

메트릭은 항상 켜져 있다. 트레이스와 로그 파일은 환경변수로 켠다.

메트릭은 Alloy가 가져가고(pull), 트레이스는 앱이 보낸다(push).
`spring-boot-starter-opentelemetry`에는 OTLP 메트릭 전송기도 들어 있어서 그대로 두면 앱이 메트릭을 `:4318/v1/metrics`로도 보내려고 한다.
같은 메트릭을 두 번 처리하지 않게 `application.yml`에서 `management.otlp.metrics.export.enabled: false`로 꺼 두었다.
push로 바꾸려면 이 값을 켜고, Alloy의 `otelcol.receiver.otlp` 출력에 `metrics`를 연결한 뒤 `prometheus.scrape`를 뺀다.

```bash
cd backend
OTLP_TRACING_ENABLED=true LOG_FILE=logs/backend.log ./gradlew bootRun
```

| 변수 | 기본값 | 설명 |
|---|---|---|
| `MANAGEMENT_PORT` | 8081 | Actuator 관리 포트. 외부에 열지 않는다 |
| `OTLP_TRACING_ENABLED` | false | 트레이스를 Alloy로 보낼지 |
| `OTLP_TRACING_ENDPOINT` | http://localhost:4318/v1/traces | Alloy OTLP HTTP 주소 |
| `TRACING_SAMPLING_PROBABILITY` | 1.0 | 트레이스를 만들 요청 비율 |
| `LOG_FILE` | (없음) | 지정하면 JSON(logstash) 형식으로 파일에 남긴다 |
| `APP_ENV` | local | 트레이스의 deployment.environment |

로그에는 `traceId`, `spanId`가 들어가서 Grafana에서 로그와 트레이스를 오갈 수 있다.
`/actuator` 요청은 트레이스를 만들지 않는다 (`ObservationConfig`).

## 서버에 둘 때

- 서버마다 Alloy를 하나 두고, 저장소(Prometheus, Loki, Tempo)와 Grafana는 한 곳에 둔다.
  각 Alloy의 출구 주소(`prometheus.remote_write`, `loki.write`, `otelcol.exporter.otlp`)를 중앙 저장소로 바꾼다.
- 트레이스가 많으면 `alloy/config.alloy`의 batch 앞에 `otelcol.processor.tail_sampling`을 넣어서 에러와 느린 요청 위주로 남긴다.
- 보관 기간은 Prometheus 15일(`PROMETHEUS_RETENTION`), Loki/Tempo 14일(각 config).
- Grafana 관리자 비밀번호는 `.env`에서 바꾼다.

## 버전을 올릴 때

- Tempo는 3.x(단일 실행, Kafka 없음)다. 2.x에서 쓰던 `tempo-data` 볼륨으로 시작이 안 되면 로컬 데이터라 지우고 다시 띄운다: `docker compose down -v && docker compose up -d`
- 이미지 버전은 Dependabot이 올린다. CI(`infra` 워크플로)가 스택을 띄워 준비 상태와 트레이스 저장, 조회까지 확인한다
