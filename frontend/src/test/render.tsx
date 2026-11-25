import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { render } from "@testing-library/react";
import type { ReactElement } from "react";
import { MemoryRouter, useLocation } from "react-router";

// 테스트용 render. 새 QueryClient(캐시가 섞이지 않게, 실패하면 바로 오류 화면)와 MemoryRouter 로 감싼다.
// route 로 시작 주소(예: "/?cursor=abc")를 정하고, 화면 이동은 data-testid="location" 의 값으로 확인한다
export function renderWithQuery(ui: ReactElement, { route = "/" }: { route?: string } = {}) {
  const queryClient = new QueryClient({
    defaultOptions: { queries: { retry: false }, mutations: { retry: false } },
  });
  return {
    queryClient,
    ...render(
      <QueryClientProvider client={queryClient}>
        <MemoryRouter initialEntries={[route]}>
          {ui}
          <LocationDisplay />
        </MemoryRouter>
      </QueryClientProvider>,
    ),
  };
}

function LocationDisplay() {
  const location = useLocation();
  return <output data-testid="location">{location.pathname + location.search}</output>;
}
