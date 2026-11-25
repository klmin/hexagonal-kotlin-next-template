---
name: add-frontend-feature
description: 백엔드 API를 사용하는 React 화면(조회 목록, 생성/수정 폼 등)을 추가한다. 새 페이지나 기존 페이지에 데이터 기능을 붙일 때 사용.
---

# 프론트 기능 추가

참고 구현: `frontend/src/lib/api/items.ts`, `frontend/src/features/items/`, `frontend/src/pages/`, `frontend/src/app.tsx`

## 0. 구조
React + Vite SPA(CSR)다. 서버 코드가 없고, 브라우저가 같은 오리진의 `/api`를 부른다.
- 경로: `src/app.tsx`의 `<Routes>`에 `<Route>`를 추가한다. 화면 컴포넌트는 `src/pages/<이름>-page.tsx`
- 기능 코드: `src/features/<기능>/` (목록, 폼, `actions.ts`)
- API: `src/lib/api/<기능>.ts`
- React Router 문서는 `frontend/node_modules/react-router/docs/` (선언형 모드: `start/declarative/`)

## 1. API 클라이언트
- 타입은 `src/lib/api/schema.d.ts`(생성 파일)의 `components["schemas"]`에서 가져온다. 스펙이 오래됐으면 백엔드에서 `./gradlew updateOpenApi`, 여기서 `npm run api:types`.
- `src/lib/api/<기능>.ts`에 타입, API 함수, 쿼리 키(`itemKeys` 참고)를 둔다.
- 호출은 `src/lib/api/client.ts`의 `request<T>()`로 한다. 경로는 `/api/...`만 된다 (같은 오리진, 타임아웃, 오류 변환 포함). 조회 함수는 `signal`을 받아 넘긴다.
- 실패하면 `ApiError`(Problem Details의 `detail`, 필드 오류 포함)가 던져진다. 화면 메시지는 `errorMessage()`로 만든다.
- URL에서 받은 id, 커서는 그대로 경로에 넣지 않고 검증한다 (`parseItemId`, `isCursor`).

## 2. 조회 (useQuery)
- 데이터를 쓰는 컴포넌트에서 `useQuery({ queryKey, queryFn: ({ signal }) => ... })`로 읽는다 (`features/items/item-list.tsx` 참고).
- `isPending`이면 로딩 표시, 오류면 컴포넌트 안에서 메시지를 보여 준다. 페이지 전체를 깨뜨리지 않는다.
- 경로 값은 `useParams()`(예: `/items/:id/edit`), 쿼리는 `useSearchParams()`로 읽는다 (`pages/edit-item-page.tsx` 참고). 백엔드 404는 `<NotFoundMessage />`로 보여 준다.

## 3. 변경 (actions.ts + useActionState)
- `actions.ts`(서버 코드 아님)에 `submitXxx(formData) => Promise<State>` 형태로 입력 검증과 API 호출을 둔다.
- 예상 가능한 오류(검증, 4xx, 연결 실패)는 throw하지 않고 `{ error, values }`로 반환한다. 성공은 `{ ok: true }`.
- 폼 컴포넌트에서 `useActionState(async (_prev, formData) => ..., initialState)`로 연결하고 `pending`으로 버튼을 비활성화한다.
- 성공하면 `useQueryClient().invalidateQueries({ queryKey: <기능>Keys.all })`로 다시 불러온다. 다른 화면으로 가야 하면 `useNavigate()`.
- 대상 id는 클로저로 넘긴다 (`delete-item-button.tsx`, `edit-item-form.tsx` 참고).

## 4. 규칙
- 백엔드는 같은 오리진의 `/api`로만 부른다. 백엔드 주소를 코드에 넣지 않는다. 새 백엔드 경로가 `/api` 밖이면 `frontend/nginx/default.conf.template`과 `vite.config.ts`의 `server.proxy`에도 추가한다.
- 빌드 결과는 누구나 받아 볼 수 있다. 비밀값을 코드나 `VITE_` 변수에 넣지 않는다.
- UI는 `src/components/ui/`의 shadcn/ui 컴포넌트(Button, Input, Label, Textarea, Card)를 먼저 쓴다. 없는 컴포넌트는 `cd frontend && npx shadcn@latest add <이름>`으로 추가한다.
- 링크는 React Router의 `<Link to>`. 버튼 모양으로 보이게 하려면 `<Button asChild><Link .../></Button>`을 쓴다.
- 입력은 `useId()`로 id를 만들어 `<Label htmlFor>`와 연결한다 (`create-item-form.tsx` 참고). 테스트가 `getByLabelText`로 찾는다.
- 색은 `zinc-500` 같은 고정 색 대신 테마 토큰(`text-muted-foreground`, `text-destructive`, `bg-primary` 등)을 쓴다. 다크 모드는 토큰이 OS 설정에 맞게 바뀐다(`globals.css`).

## 5. 테스트
- `actions.ts`, API 함수는 `fetch`를 `vi.stubGlobal`로 바꿔 테스트한다 (`actions.test.ts`, `items.test.ts`).
- 화면 컴포넌트는 `src/test/render.tsx`의 `renderWithQuery`로 그린다. QueryClient와 `MemoryRouter`로 감싸 주고, 시작 주소는 `{ route }`, 이동 결과는 `getByTestId("location")`으로 확인한다 (`item-list.test.tsx`, `app.test.tsx` 참고).
- 화면 흐름은 `e2e/`에 Playwright 테스트를 추가한다 (`items.spec.ts` 참고). 테스트 데이터는 `request.post("/api/...")`로 만든다.

## 6. 검증
```bash
cd frontend && npm run format:check && npm run lint && npm run typecheck && npm test && npm run build
```
가능하면 `npm run dev`(백엔드는 `bootRun`)로 띄워 실제 백엔드와 함께 생성, 조회, 오류 표시를 확인한다. 새 경로는 주소창에 바로 넣고 새로고침해도 나오는지 본다.
