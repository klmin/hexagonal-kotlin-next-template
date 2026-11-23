# frontend

Next.js 16 (App Router, Cache Components) / React 19 / TypeScript / Tailwind 4 / shadcn/ui

## 실행

```bash
cp .env.example .env.local   # BACKEND_URL
npm ci
npm run dev
```

백엔드가 `BACKEND_URL`(기본 http://localhost:8080)에서 떠 있어야 목록이 나온다. 기본 포트 그대로면 `.env.local`은 없어도 된다.
설정값 전체는 [docs/configuration.md](../docs/configuration.md).

## 스크립트

- `npm run dev` - 개발 서버
- `npm run build` - 프로덕션 빌드
- `npm run lint` - ESLint (Next 16부터 `next lint` 없음)
- `npm run format` / `npm run format:check` - Prettier (Tailwind 클래스 정렬 포함)
- `npm test` - Vitest
- `npm run test:e2e` - Playwright (앱이 떠 있어야 함)
- `npm run typecheck` - `next typegen && tsc --noEmit`
- `npm run api:types` - `../openapi/openapi.json`에서 `src/lib/api/schema.d.ts` 생성

## 테스트

Vitest + Testing Library. 파일은 대상 옆에 `*.test.ts(x)`로 둔다.
async 서버 컴포넌트는 Vitest가 지원하지 않아서 E2E로 확인한다.

E2E는 Playwright로 `e2e/`에 둔다. 떠 있는 앱을 대상으로 돈다.

```bash
# 루트에서 전체 스택 실행
(cd backend && ./gradlew bootJar)
docker compose up -d --build --wait

# frontend
npx playwright install chromium   # 처음 한 번
npm run test:e2e                  # 기본 대상 http://localhost:3000 (E2E_BASE_URL로 변경)
                                  # 테스트 데이터를 API로 만드는 테스트는 http://localhost:8080 (E2E_API_URL)
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

- 설정은 `components.json`, 테마 색은 `src/app/globals.css`의 CSS 변수
- 다크 모드는 OS 설정(`prefers-color-scheme`)을 따른다. 화면에서 직접 바꾸게 하려면 `next-themes`를 넣고 `@custom-variant dark`를 `.dark` 클래스 방식으로 바꾼다
- `global-error.tsx`는 앱 전체가 깨졌을 때 뜨는 화면이라 컴포넌트를 쓰지 않는다

## Docker

```bash
docker build -t frontend .
docker run -p 3000:3000 -e BACKEND_URL=http://<backend>:8080 frontend
```

## 구조

```
src/
├── app/
│   ├── page.tsx                 # 목록 + 생성 폼
│   ├── error.tsx                # 오류 화면 (retry)
│   ├── not-found.tsx
│   ├── loading.tsx
│   └── items/
│       ├── actions.ts           # Server Action (생성/수정 후 첫 페이지로 redirect, 삭제 후 refresh)
│       ├── create-item-form.tsx # 폼 (useActionState)
│       ├── item-list.tsx        # 서버 컴포넌트, 커서 페이징(?cursor=), 수정 링크/삭제 버튼
│       └── [id]/edit/
│           ├── page.tsx         # 동적 경로. params 를 Suspense 안에서 await
│           └── edit-item-form.tsx
├── components/ui/               # shadcn/ui 컴포넌트 (npx shadcn add 로 추가)
├── lib/
│   ├── utils.ts                 # cn() (shadcn)
│   ├── env.ts                   # 서버 환경변수 검증 (zod)
│   └── api/
│       ├── client.ts            # fetch 공통 (타임아웃, Problem Details -> ApiError)
│       ├── items.ts             # Item API
│       └── schema.d.ts          # OpenAPI에서 생성한 타입
└── test/                        # Vitest 설정
```

- 백엔드 호출은 서버에서만 한다. 브라우저는 백엔드 주소를 모르니 CORS 설정이 필요 없다.
- 환경변수는 `src/lib/env.ts`에서 검증한다. 새 변수는 여기 스키마에 추가한다.
- `cacheComponents`가 켜져 있어서 캐시하지 않는 데이터는 `<Suspense>` 안에서 읽어야 빌드가 된다.
