import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";

import { ApiError, request } from "./client";

vi.mock("next/server", () => ({ connection: async () => {} }));

function mockFetch(status: number, body?: unknown) {
  const fetchMock = vi.fn().mockResolvedValue(
    new Response(body === undefined ? null : JSON.stringify(body), {
      status,
      headers: body === undefined ? {} : { "Content-Type": "application/json" },
    }),
  );
  vi.stubGlobal("fetch", fetchMock);
  return fetchMock;
}

beforeEach(() => {
  // 셸에 BACKEND_URL 이 있어도 결과가 같게
  vi.stubEnv("BACKEND_URL", "http://localhost:8080");
});

afterEach(() => {
  vi.unstubAllGlobals();
  vi.unstubAllEnvs();
});

describe("request", () => {
  it("BACKEND_URL 기준으로 호출하고 JSON을 돌려준다", async () => {
    const fetchMock = mockFetch(200, [{ id: 1 }]);

    await expect(request("/api/items")).resolves.toEqual([{ id: 1 }]);
    expect(String(fetchMock.mock.calls[0][0])).toBe("http://localhost:8080/api/items");
  });

  it("본문이 없는 응답(204)은 undefined를 돌려준다", async () => {
    mockFetch(204);

    await expect(request("/api/items/1", { method: "DELETE" })).resolves.toBeUndefined();
  });

  it("Headers 객체로 넘긴 헤더도 보내고 Content-Type 기본값을 붙인다", async () => {
    const fetchMock = mockFetch(200, {});

    await request("/api/items", { headers: new Headers({ Authorization: "Bearer t" }) });

    const headers = (fetchMock.mock.calls[0][1] as RequestInit).headers as Headers;
    expect(headers.get("Authorization")).toBe("Bearer t");
    expect(headers.get("Content-Type")).toBe("application/json");
  });

  it("호출하는 쪽 signal로 취소할 수 있다", async () => {
    const fetchMock = mockFetch(200, {});
    const controller = new AbortController();

    await request("/api/items", { signal: controller.signal });
    controller.abort();

    expect(((fetchMock.mock.calls[0][1] as RequestInit).signal as AbortSignal).aborted).toBe(true);
  });

  it("Problem Details를 ApiError로 바꾸고 필드 오류를 메시지에 붙인다", async () => {
    mockFetch(400, {
      status: 400,
      detail: "입력값을 확인해 주세요.",
      errors: [{ field: "name", message: "must not be blank" }],
    });

    const error = await request("/api/items").catch((e: unknown) => e);

    expect(error).toBeInstanceOf(ApiError);
    expect((error as ApiError).status).toBe(400);
    expect((error as ApiError).message).toBe("입력값을 확인해 주세요. (name: must not be blank)");
  });
});
