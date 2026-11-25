import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { useState } from "react";

import { ApiError } from "@/lib/api/client";

// 백엔드가 응답한 오류(4xx, 5xx, nginx 의 502/504 포함)는 다시 보내지 않는다. 백엔드가 내려가 있으면
// nginx 연결 시간 초과(5초)만큼 기다리므로, 재시도하면 오류 화면이 그만큼 늦게 뜬다. 네트워크 오류만 한 번 다시 시도한다
function shouldRetry(failureCount: number, error: unknown): boolean {
  if (error instanceof ApiError) {
    return false;
  }
  return failureCount < 1;
}

export function Providers({ children }: { children: React.ReactNode }) {
  // 컴포넌트마다 새로 만들지 않게 state 로 한 번만 만든다
  const [queryClient] = useState(
    () => new QueryClient({ defaultOptions: { queries: { retry: shouldRetry } } }),
  );
  return <QueryClientProvider client={queryClient}>{children}</QueryClientProvider>;
}
