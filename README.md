# hexagonal-kotlin-react-template

Spring Boot(헥사고날, Kotlin) 백엔드 + React(Vite) 프론트엔드 모노레포 템플릿.
[hexagonal-java-react-template](https://github.com/klmin/hexagonal-java-react-template)의 백엔드를 코틀린으로 옮긴 연습용 버전이다. 자바 버전과 다른 점은 [docs/kotlin-vs-java.md](docs/kotlin-vs-java.md).
샘플 기능(Item 생성/조회)이 DB부터 화면까지 연결돼 있으니 같은 방식으로 기능을 추가하면 된다.

- `backend/` - Spring Boot 4.2, Kotlin 2.4 (JDK 25), Gradle(Kotlin DSL), PostgreSQL, Flyway
- `frontend/` - React 19 + Vite (CSR, SPA), React Router, TanStack Query, TypeScript, Tailwind 4. 배포는 nginx가 정적 파일을 서빙하고 `/api`를 백엔드로 넘긴다
- `compose.yaml` - 전체 스택을 이미지로 실행 (E2E, 로컬 확인)
- `docs/configuration.md` - 설정값 (환경변수, .env)
- `docs/authentication.md` - 인증을 붙일 때 위치 규칙 (인증 코드는 없음)
- `docs/kotlin-vs-java.md` - 자바 버전과 다른 점 (공부용 대응표)
- `openapi/` - API 스펙 (백엔드에서 생성, 프론트 타입 생성에 사용)
- `infra/observability/` - 모니터링 스택 (Prometheus, Loki, Tempo, Grafana, Alloy)
- `.github/` - CI, CodeQL, Dependabot, PR/이슈 템플릿

## 필요한 것

- JDK 25
- Node.js 22.22.2+, 24.15+ 또는 26+ (CI와 Docker 이미지는 26 LTS. 25는 Vitest가 지원하지 않음)
- Docker

## 실행

로컬 실행에 넣어야 하는 설정값은 없다. 바꿀 수 있는 값과 운영에서 넣을 값은 [docs/configuration.md](docs/configuration.md).
IntelliJ에서는 `.run/`의 실행 설정(backend bootRun, frontend dev 등)을 바로 쓸 수 있다.

```bash
# 백엔드 (compose.yaml의 PostgreSQL이 같이 뜸)
cd backend
./gradlew bootRun

# 프론트엔드
cd frontend
cp .env.example .env.local
npm ci
npm run dev
```

http://localhost:3000

전체를 이미지로 띄울 때:

```bash
(cd backend && ./gradlew bootJar)
docker compose up -d --build --wait
```

모니터링은 [infra/observability](infra/observability/README.md) 참고.

## 새 프로젝트로 쓸 때

1. GitHub에서 **Use this template**으로 저장소 생성 후 clone
2. 이름, 패키지, 소유자 변경

   ```bash
   node scripts/init-project.mjs --name my-shop --package com.acme.shop --owner my-github-id
   ```

   패키지 폴더 이동, `group`, README 제목, 브라우저 탭 제목, compose 이름, CODEOWNERS, LICENSE를 바꾼다. `--dry-run`으로 미리 볼 수 있다.
   조직 저장소면 `--owner`에 팀을 준다(`--owner acme/backend`). CODEOWNERS는 조직 ID만으로는 동작하지 않는다.
3. `git diff` 확인 후 `cd backend && ./gradlew build` (Docker 필요)
4. 샘플 `item` 기능은 참고 후 삭제하거나 실제 도메인으로 교체
5. 저장소 설정 (템플릿에서 복사되지 않음)
   - **PR 자동 리뷰**: PR에 push할 때마다 Codex 리뷰를 요청하려면(`.github/workflows/codex-review-on-push.yml`) Actions secret `CODEX_TRIGGER_PAT`을 등록한다.
     Codex 계정과 연결된 본인 GitHub의 fine-grained token(이 저장소만, Pull requests, Issues: Read and write).
     등록하지 않으면 이 워크플로는 요청 없이 건너뛴다
   - **main 보호 규칙**: `backend`, `frontend`, `e2e`, `template-init`, `infra` 워크플로는 바뀐 경로에서만 돈다.
     이 검사들을 필수(required)로 지정하면 해당 경로를 건드리지 않은 PR은 검사를 계속 기다리며 머지할 수 없다.
     필수로는 모든 PR에서 도는 CodeQL `analyze (...)`만 지정하고, 나머지는 리뷰할 때 확인한다
   - **CodeQL**: 비공개 저장소는 GitHub Code Security(Advanced Security)가 있어야 결과를 올릴 수 있다. 없으면 `codeql` 워크플로가 실패하니 `.github/workflows/codeql.yml`을 지운다

작업 규칙은 [AGENTS.md](AGENTS.md) 참고.

## License

[MIT](LICENSE)
