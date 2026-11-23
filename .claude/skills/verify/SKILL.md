---
name: verify
description: 백엔드와 프론트엔드의 빌드, 테스트, 린트, 타입 검사를 CI와 같은 순서로 실행하고 결과를 요약한다. PR 전에 또는 "전체 검증해줘" 요청 시 사용.
---

# 전체 검증

CI(`.github/workflows/backend.yml`, `frontend.yml`)와 같은 검사를 로컬에서 실행한다.
변경된 쪽만 검사해도 되지만, 양쪽을 모두 바꿨거나 확실하지 않으면 둘 다 실행한다.

## backend
```bash
cd backend && ./gradlew build
```
- JDK 25가 필요하다. 기본 JDK가 다르면 `JAVA_HOME`을 25로 지정해 실행한다.
- 출력 마지막의 `Tests: N, passed: ..., skipped: ...` 요약을 확인한다.
- skipped가 있으면 Docker가 꺼져 Testcontainers 테스트가 빠진 것이다. 결과 보고에 그 사실을 적는다.

## frontend
```bash
cd frontend && npm ci && npm run format:check && npm run lint && npm run typecheck && npm test && npm run build
```
- `node_modules`가 이미 최신이면 `npm ci`는 생략해도 된다.

## e2e (화면 흐름을 바꿨을 때)
```bash
(cd backend && ./gradlew bootJar)
docker compose up -d --build --wait
cd frontend && npm run test:e2e
docker compose down -v
```

## 보고
- 각 단계의 통과/실패, 실패한 테스트, 오류 메시지 핵심, 건너뛴 항목을 짧게 정리한다.
- 실패를 고친 경우 무엇을 고쳤는지 함께 적는다. 검사를 끄거나 테스트를 삭제해서 통과시키지 않는다.
