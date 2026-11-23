---
name: add-frontend-feature
description: 백엔드 API를 사용하는 Next.js 화면(조회 목록, 생성/수정 폼 등)을 추가한다. 새 페이지나 기존 페이지에 데이터 기능을 붙일 때 사용.
---

# 프론트 기능 추가

참고 구현: `frontend/src/lib/api/items.ts`, `frontend/src/app/items/`, `frontend/src/app/page.tsx`

## 0. 먼저 문서 확인
Next.js 16은 이전 버전과 API가 다르다. 쓰려는 기능의 문서를 `frontend/node_modules/next/dist/docs/01-app/`에서 확인한다.
- 데이터 조회, 캐시: `01-getting-started/08-caching.md`
- 변경(Server Action): `01-getting-started/07-mutating-data.md`
- 오류 처리: `01-getting-started/10-error-handling.md`

## 1. API 클라이언트
- 타입은 `src/lib/api/schema.d.ts`(생성 파일)의 `components["schemas"]`에서 가져온다. 스펙이 오래됐으면 백엔드에서 `./gradlew updateOpenApi`, 여기서 `npm run api:types`.
- `src/lib/api/<기능>.ts`에 타입과 함수를 추가한다. 첫 줄은 `import "server-only";`.
- 호출은 `src/lib/api/client.ts`의 `request<T>()`로 한다 (요청 시점 환경변수, 타임아웃, 오류 변환 포함).
- 실패하면 `ApiError`(Problem Details의 `detail`, 필드 오류 포함)가 던져진다.

## 2. 조회 (서버 컴포넌트)
- `src/app/<경로>/` 아래에 async 서버 컴포넌트로 만든다.
- 캐시하지 않는 데이터는 페이지에서 `<Suspense fallback={...}>`로 감싼다. 감싸지 않으면 `next build`가 실패한다.
- 백엔드 장애 시 페이지 전체가 깨지지 않도록 컴포넌트 안에서 오류 메시지를 보여 주거나 `error.tsx`를 둔다.
- 동적 경로의 params는 `PageProps<'/경로/[id]'>`로 받는다. params는 요청 시점 데이터라 페이지에서 await 하지 말고 Suspense 안의 자식 컴포넌트로 넘겨서 await 한다 (`items/[id]/edit/page.tsx` 참고). 백엔드 404는 `notFound()`로 바꾼다.
- JSX를 try/catch 안에서 만들지 않는다 (lint 오류). 데이터 조회만 try 안에서 한다.

## 3. 변경 (Server Action)
- `actions.ts` (`"use server"`)에 `(prevState, formData) => Promise<State>` 형태로 작성한다.
- 예상 가능한 오류(검증, 4xx)는 throw하지 않고 `{ error }`로 반환한다.
- 성공하면 `refresh()`(`next/cache`)를 호출한다. 다른 페이지로 이동해야 하면 `redirect()`를 쓴다.
- 폼은 클라이언트 컴포넌트에서 `useActionState`(`react`)로 연결하고 `pending`으로 버튼을 비활성화한다.
- 대상 id 같은 값은 `action.bind(null, id)`로 넘긴다 (`deleteItemAction`, `updateItemAction` 참고).
- `redirect()`는 예외로 동작하므로 try/catch 밖에서 부른다.

## 4. 규칙
- 브라우저에서 백엔드를 직접 호출하지 않는다. 서버 전용 값을 `NEXT_PUBLIC_`으로 만들지 않는다.
- UI는 `src/components/ui/`의 shadcn/ui 컴포넌트(Button, Input, Label, Textarea, Card)를 먼저 쓴다. 없는 컴포넌트는 `cd frontend && npx shadcn@latest add <이름>`으로 추가한다.
- 링크를 버튼 모양으로 보이게 하려면 `<Button asChild><Link .../></Button>`을 쓴다.
- 입력은 `useId()`로 id를 만들어 `<Label htmlFor>`와 연결한다 (`create-item-form.tsx` 참고). 테스트가 `getByLabelText`로 찾는다.
- 색은 `zinc-500` 같은 고정 색 대신 테마 토큰(`text-muted-foreground`, `text-destructive`, `bg-primary` 등)을 쓴다. 다크 모드는 토큰이 OS 설정에 맞게 바뀐다(`globals.css`).

## 5. 테스트
- 클라이언트 컴포넌트, 유틸은 Vitest로 (`create-item-form.test.tsx`, `client.test.ts` 참고). Server Action은 `vi.mock`으로 대체한다.
- async 서버 컴포넌트와 화면 흐름은 `e2e/`에 Playwright 테스트를 추가한다 (`items.spec.ts` 참고). Next.js 라우트 안내 요소가 `role=alert`이라 alert는 범위를 좁혀서 찾는다. 이동한 뒤에는 이전 화면이 숨겨진 채 남아 있으므로, 같은 라벨이 두 화면에 있으면 현재 화면의 `main` 안에서 찾는다.

## 6. 검증
```bash
cd frontend && npm run format:check && npm run lint && npm run typecheck && npm test && npm run build
```
가능하면 `npm run dev`로 띄워 실제 백엔드와 함께 생성, 조회, 오류 표시를 확인한다.
