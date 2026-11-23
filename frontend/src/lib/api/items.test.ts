import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";

import { deleteItem, isItemId } from "./items";

vi.mock("next/server", () => ({ connection: async () => {} }));

beforeEach(() => {
  vi.stubEnv("BACKEND_URL", "http://localhost:8080");
});

afterEach(() => {
  vi.unstubAllGlobals();
  vi.unstubAllEnvs();
});

describe("isItemId", () => {
  it("양의 정수만 허용한다", () => {
    expect(isItemId(1)).toBe(true);
    expect(isItemId(0)).toBe(false);
    expect(isItemId(-1)).toBe(false);
    expect(isItemId(1.5)).toBe(false);
    expect(isItemId("1")).toBe(false);
    expect(isItemId("1/../../actuator")).toBe(false);
  });
});

describe("deleteItem", () => {
  it("올바른 id면 /api/items/{id} 로 DELETE 한다", async () => {
    const fetchMock = vi.fn().mockResolvedValue(new Response(null, { status: 204 }));
    vi.stubGlobal("fetch", fetchMock);

    await deleteItem(5);

    expect(String(fetchMock.mock.calls[0][0])).toBe("http://localhost:8080/api/items/5");
    expect((fetchMock.mock.calls[0][1] as RequestInit).method).toBe("DELETE");
  });

  it("조작된 id는 백엔드를 호출하지 않고 실패한다", async () => {
    const fetchMock = vi.fn();
    vi.stubGlobal("fetch", fetchMock);

    expect(() => deleteItem("1/../../actuator" as unknown as number)).toThrow(/invalid item id/);
    expect(fetchMock).not.toHaveBeenCalled();
  });
});
