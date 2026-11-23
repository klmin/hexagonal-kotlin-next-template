import "server-only";

import { connection } from "next/server";

import { serverEnv } from "@/lib/env";

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
  // 빌드 시점이 아니라 요청 시점의 환경변수를 읽도록
  await connection();
  const url = new URL(path, serverEnv().BACKEND_URL);

  // init.headers 가 Headers 객체여도 값이 빠지지 않게 Headers 로 합친다
  const headers = new Headers(init?.headers);
  if (!headers.has("Content-Type")) {
    headers.set("Content-Type", "application/json");
  }
  // 백엔드가 응답하지 않을 때 화면이 계속 로딩 상태로 남지 않게. 호출하는 쪽 signal 과 함께 쓴다
  const timeout = AbortSignal.timeout(10_000);
  const signal = init?.signal ? AbortSignal.any([timeout, init.signal]) : timeout;

  const res = await fetch(url, { ...init, headers, signal });
  if (!res.ok) {
    throw await toApiError(res);
  }
  // 204 No Content 등 본문이 없는 응답
  const text = await res.text();
  return (text ? JSON.parse(text) : undefined) as T;
}
