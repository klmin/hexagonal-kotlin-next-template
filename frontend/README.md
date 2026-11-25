# frontend

React 19 + Vite (SPA, CSR) / React Router / TanStack Query / TypeScript / Tailwind 4 / shadcn/ui

`vite build`가 정적 파일(`dist/`)을 만든다. Node 서버 없이 nginx가 서빙하고, 브라우저는 같은 오리진의 `/api`로 백엔드를 부른다.

```
브라우저 --/, /items/1/edit ...--> nginx (dist/ 정적 파일, 없는 경로는 index.html)
         --/api/...-------------> nginx --> backend (BACKEND_URL)
```

## 실행

```bash
cp .env.example .env.local   # DEV_BACKEND_URL (기본값이면 생략)
npm ci
npm run dev                  # http://localhost:3000
```

개발 서버는 `/api/*` 요청을 `DEV_BACKEND_URL`(기본 http://localhost:8080)로 넘긴다(`vite.config.ts`의 `server.proxy`). 백엔드가 떠 있어야 목록이 나온다. 기본 포트 그대로면 `.env.local`은 없어도 된다.
설정값 전체는 [docs/configuration.md](../docs/configuration.md).

## 스크립트

- `npm run dev` - 개발 서버 (3000 포트)
- `npm run build` - 정적 파일 빌드 (`dist/`)
- `npm run preview` - 빌드 결과를 로컬에서 확인 (`/api` 프록시 포함)
- `npm run lint` - ESLint
- `npm run format` / `npm run format:check` - Prettier (Tailwind 클래스 정렬 포함)
- `npm test` - Vitest
- `npm run test:e2e` - Playwright (앱이 떠 있어야 함)
- `npm run typecheck` - `tsc --noEmit`
- `npm run api:types` - `../openapi/openapi.json`에서 `src/lib/api/schema.d.ts` 생성

## 구조

```
index.html                       # 진입 HTML (탭 제목)
vite.config.ts                   # 플러그인, 개발 서버 /api 프록시, Vitest 설정
nginx/default.conf.template      # Docker 이미지의 nginx 설정
src/
├── main.tsx                     # 루트: ErrorBoundary, QueryClient, BrowserRouter
├── app.tsx                      # 화면 경로 (React Router <Routes>)
├── providers.tsx                # QueryClient (백엔드 오류 응답은 재시도 안 함)
├── error-boundary.tsx           # 예상하지 못한 렌더링 오류 화면
├── index.css                    # Tailwind, 테마 색
├── pages/                       # 경로별 화면
│   ├── home-page.tsx            # / 목록 + 생성 폼
│   ├── edit-item-page.tsx       # /items/:id/edit
│   └── not-found-page.tsx       # 그 외 경로
├── features/items/              # 기능별 컴포넌트와 폼 처리
│   ├── actions.ts               # 폼 제출 처리 (입력 검증, API 호출, 오류를 상태로 반환)
│   ├── create-item-form.tsx     # useActionState, 성공하면 목록 쿼리 무효화
│   ├── edit-item-form.tsx
│   ├── delete-item-button.tsx
│   └── item-list.tsx            # useQuery, 커서 페이징(?cursor=)
├── components/
│   ├── not-found-message.tsx    # 없는 경로, 없는 항목 공통
│   └── ui/                      # shadcn/ui 컴포넌트 (npx shadcn add 로 추가)
├── lib/
│   ├── utils.ts                 # cn() (shadcn)
│   └── api/
│       ├── client.ts            # fetch 공통 (/api 경로만, 타임아웃, Problem Details -> ApiError)
│       ├── items.ts             # Item API, 쿼리 키(itemKeys), id/커서 검증
│       └── schema.d.ts          # OpenAPI에서 생성한 타입
└── test/                        # Vitest 설정, renderWithQuery
```

- 브라우저는 같은 오리진의 `/api`만 부른다. 백엔드 주소가 화면 코드에 없고 CORS 설정도 필요 없다.
- 빌드 결과는 누구나 받아 볼 수 있는 정적 파일이다. 비밀값(키, 토큰)을 프론트 코드나 `VITE_` 변수에 넣지 않는다. `VITE_` 변수는 빌드할 때 코드에 그대로 들어간다.
- 새 화면은 `src/pages/`에 만들고 `src/app.tsx`에 `<Route>`를 추가한다. 경로 값(`:id`)은 `useParams()`, 쿼리는 `useSearchParams()`로 읽는다.

## 테스트

Vitest + Testing Library. 파일은 대상 옆에 `*.test.ts(x)`로 둔다.
화면 컴포넌트는 `src/test/render.tsx`의 `renderWithQuery`로 그린다. QueryClient와 `MemoryRouter`로 감싸고, 시작 주소는 `{ route: "/?cursor=abc" }`로 정한다.
백엔드는 `fetch`를 `vi.stubGlobal`로 바꾼다 (`item-list.test.tsx`, `app.test.tsx` 참고).

E2E는 Playwright로 `e2e/`에 둔다. 떠 있는 앱을 대상으로 돈다.

```bash
# 루트에서 전체 스택 실행
(cd backend && ./gradlew bootJar)
docker compose up -d --build --wait

# frontend
npx playwright install chromium   # 처음 한 번
npm run test:e2e                  # 기본 대상 http://localhost:3000 (E2E_BASE_URL로 변경)
                                  # 테스트 데이터도 같은 주소의 /api 로 만든다
```

CI(`.github/workflows/e2e.yml`)도 같은 방식으로 돈다.

## API 타입

백엔드 API 타입은 손으로 쓰지 않고 OpenAPI 스펙에서 생성한다.
백엔드에서 `./gradlew updateOpenApi`로 스펙이 바뀌면 여기서 `npm run api:types`를 돌리고 같이 커밋한다.
CI가 생성 결과와 커밋된 파일이 다르면 실패시킨다.

```ts
import type { components } from "./schema";
type Item = components["schemas"]["ItemResponse"];
```

## UI 컴포넌트

[shadcn/ui](https://ui.shadcn.com)(Radix, new-york 스타일)를 쓴다. 컴포넌트는 패키지가 아니라 코드로 `src/components/ui/`에 들어 있어서 직접 고쳐도 된다.

```bash
npx shadcn@latest add dialog   # 필요한 컴포넌트만 추가
```

- 설정은 `components.json`, 테마 색은 `src/index.css`의 CSS 변수. 글꼴은 Geist(`@fontsource-variable/geist`)
- 다크 모드는 OS 설정(`prefers-color-scheme`)을 따른다. 화면에서 직접 바꾸게 하려면 `@custom-variant dark`를 `<html>`의 `.dark` 클래스 방식으로 바꾼다

## Docker

```bash
docker build -t frontend .
docker run -p 3000:3000 -e BACKEND_URL=http://<backend>:8080 frontend
```

- 이미지는 `nginxinc/nginx-unprivileged`(루트 아님)이고 설정은 `nginx/default.conf.template`
- `BACKEND_URL`은 컨테이너 시작 때 nginx 설정에 들어간다. 이미지를 다시 빌드하지 않아도 바뀐다. 경로 없이 `http://host:port`만 쓴다
- 백엔드 주소는 요청 때마다 DNS로 찾는다(10초 캐시). 백엔드가 아직 없어도 nginx는 뜨고 `/api`만 502를 준다. nginx는 DNS 검색 도메인을 쓰지 않으므로 k8s에서는 `http://backend.<namespace>.svc.cluster.local:8080`처럼 FQDN을 쓴다
- 파일이 없는 경로는 `index.html`을 돌려준다(SPA). 그래서 없는 화면 경로도 HTTP 200이고, "찾을 수 없음"은 화면이 보여 준다. `/assets/`의 없는 파일은 404
- `/assets/`(파일 이름에 해시)는 오래 캐시하고 `index.html`은 매번 확인한다
- 다른 정적 호스팅(CDN, S3 등)에 올릴 때도 `dist/`를 올리고, `/api`를 백엔드로 넘기는 규칙과 없는 경로를 `index.html`로 돌려주는 규칙을 같이 설정한다
