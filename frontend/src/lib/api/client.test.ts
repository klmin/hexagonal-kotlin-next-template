import { afterEach, describe, expect, it, vi } from "vitest";

import { ApiError, errorMessage, request } from "./client";

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

afterEach(() => {
  vi.unstubAllGlobals();
});

describe("request", () => {
  it("같은 오리진의 /api 경로로 호출하고 JSON을 돌려준다", async () => {
    const fetchMock = mockFetch(200, [{ id: 1 }]);

    await expect(request("/api/items")).resolves.toEqual([{ id: 1 }]);
    expect(fetchMock.mock.calls[0][0]).toBe("/api/items");
  });

  it("/api 로 시작하지 않는 경로는 호출하지 않는다", async () => {
    const fetchMock = mockFetch(200, {});

    await expect(request("https://example.com/api/items")).rejects.toThrow(/\/api\//);
    expect(fetchMock).not.toHaveBeenCalled();
  });

  it("본문이 없는 응답(204)은 undefined를 돌려준다", async () => {
    mockFetch(204);

    await expect(request("/api/items/1", { method: "DELETE" })).resolves.toBeUndefined();
  });

  it("본문이 있으면 Content-Type 기본값을 붙이고, Headers 객체로 넘긴 헤더도 보낸다", async () => {
    const fetchMock = mockFetch(200, {});

    await request("/api/items", {
      method: "POST",
      body: "{}",
      headers: new Headers({ "X-Request-Id": "abc" }),
    });

    const headers = (fetchMock.mock.calls[0][1] as RequestInit).headers as Headers;
    expect(headers.get("X-Request-Id")).toBe("abc");
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

describe("errorMessage", () => {
  it("ApiError는 백엔드 메시지를, 그 외에는 공통 메시지를 돌려준다", () => {
    expect(errorMessage(new ApiError(409, "이미 있습니다"))).toBe("이미 있습니다");
    expect(errorMessage(new TypeError("Failed to fetch"))).toBe("백엔드에 연결할 수 없습니다.");
  });
});
