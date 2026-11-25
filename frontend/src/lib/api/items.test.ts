import { afterEach, describe, expect, it, vi } from "vitest";

import { deleteItem, isItemId, listItems, parseItemId } from "./items";

afterEach(() => {
  vi.unstubAllGlobals();
});

function mockFetch(response: Response) {
  const fetchMock = vi.fn().mockResolvedValue(response);
  vi.stubGlobal("fetch", fetchMock);
  return fetchMock;
}

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

describe("parseItemId", () => {
  it("숫자로만 된 양의 정수 문자열만 id로 바꾼다", () => {
    expect(parseItemId("12")).toBe(12);
    expect(parseItemId(null)).toBeNull();
    expect(parseItemId("")).toBeNull();
    expect(parseItemId("0")).toBeNull();
    expect(parseItemId("012")).toBeNull();
    expect(parseItemId("1e3")).toBeNull();
    expect(parseItemId("1/../../actuator")).toBeNull();
    expect(parseItemId("99999999999999999999")).toBeNull();
  });
});

describe("listItems", () => {
  it("올바른 커서만 쿼리로 붙인다", async () => {
    // 응답 본문은 한 번만 읽을 수 있어서 호출마다 새로 만든다
    const fetchMock = vi
      .fn()
      .mockImplementation(async () => Response.json({ items: [], nextCursor: null }));
    vi.stubGlobal("fetch", fetchMock);

    await listItems("abc_-1");
    await listItems("../../actuator");

    expect(fetchMock.mock.calls[0][0]).toBe("/api/items?cursor=abc_-1");
    expect(fetchMock.mock.calls[1][0]).toBe("/api/items");
  });
});

describe("deleteItem", () => {
  it("올바른 id면 /api/items/{id} 로 DELETE 한다", async () => {
    const fetchMock = mockFetch(new Response(null, { status: 204 }));

    await deleteItem(5);

    expect(fetchMock.mock.calls[0][0]).toBe("/api/items/5");
    expect((fetchMock.mock.calls[0][1] as RequestInit).method).toBe("DELETE");
  });

  it("조작된 id는 백엔드를 호출하지 않고 실패한다", async () => {
    const fetchMock = vi.fn();
    vi.stubGlobal("fetch", fetchMock);

    expect(() => deleteItem("1/../../actuator" as unknown as number)).toThrow(/invalid item id/);
    expect(fetchMock).not.toHaveBeenCalled();
  });
});
