// 브라우저에서 백엔드 API를 부른다. 경로는 같은 오리진의 /api/... 이다.
// 배포에서는 nginx 가, 개발 서버에서는 vite.config.ts 의 proxy 가 /api 를 백엔드로 넘긴다

// 백엔드 오류 응답 (RFC 9457 Problem Details)
type ProblemDetail = {
  title?: string;
  status?: number;
  detail?: string;
  errors?: { field: string; message: string }[];
};

export class ApiError extends Error {
  constructor(
    readonly status: number,
    message: string,
  ) {
    super(message);
    this.name = "ApiError";
  }
}

async function toApiError(res: Response): Promise<ApiError> {
  let message = `요청에 실패했습니다 (${res.status})`;
  try {
    const problem = (await res.json()) as ProblemDetail;
    message = problem.detail ?? problem.title ?? message;
    if (problem.errors?.length) {
      message += ` (${problem.errors.map((e) => `${e.field}: ${e.message}`).join(", ")})`;
    }
  } catch {
    // JSON이 아니면 기본 메시지 사용
  }
  return new ApiError(res.status, message);
}

export async function request<T>(path: string, init?: RequestInit): Promise<T> {
  if (!path.startsWith("/api/")) {
    throw new Error(`API 경로는 /api/ 로 시작해야 합니다: ${path}`);
  }

  // init.headers 가 Headers 객체여도 값이 빠지지 않게 Headers 로 합친다
  const headers = new Headers(init?.headers);
  if (init?.body !== undefined && !headers.has("Content-Type")) {
    headers.set("Content-Type", "application/json");
  }
  // 백엔드가 응답하지 않을 때 화면이 계속 로딩 상태로 남지 않게. 호출하는 쪽 signal 과 함께 쓴다
  const timeout = AbortSignal.timeout(10_000);
  const signal = init?.signal ? AbortSignal.any([timeout, init.signal]) : timeout;

  const res = await fetch(path, { ...init, headers, signal });
  if (!res.ok) {
    throw await toApiError(res);
  }
  // 204 No Content 등 본문이 없는 응답
  const text = await res.text();
  return (text ? JSON.parse(text) : undefined) as T;
}

// 화면에 보여 줄 오류 메시지. 백엔드가 준 메시지가 있으면 그대로, 연결 실패 등은 공통 메시지
export function errorMessage(error: unknown, fallback = "백엔드에 연결할 수 없습니다."): string {
  return error instanceof ApiError ? error.message : fallback;
}
