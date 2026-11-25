import { afterEach, describe, expect, it, vi } from "vitest";

import { submitCreateItem, submitDeleteItem, submitUpdateItem } from "./actions";

function form(values: Record<string, string>) {
  const data = new FormData();
  for (const [key, value] of Object.entries(values)) {
    data.set(key, value);
  }
  return data;
}

function mockFetch(response: Response | Error) {
  const fetchMock =
    response instanceof Error
      ? vi.fn().mockRejectedValue(response)
      : vi.fn().mockResolvedValue(response);
  vi.stubGlobal("fetch", fetchMock);
  return fetchMock;
}

afterEach(() => {
  vi.unstubAllGlobals();
});

describe("submitCreateItem", () => {
  it("이름이 공백뿐이면 백엔드를 부르지 않고 오류를 돌려준다", async () => {
    const fetchMock = mockFetch(Response.json({}));

    const state = await submitCreateItem(form({ name: "   ", description: "설명" }));

    expect(state).toEqual({
      error: "이름을 입력해 주세요.",
      values: { name: "", description: "설명" },
    });
    expect(fetchMock).not.toHaveBeenCalled();
  });

  it("앞뒤 공백을 지우고, 빈 설명은 보내지 않는다", async () => {
    const fetchMock = mockFetch(Response.json({ id: 1 }, { status: 201 }));

    const state = await submitCreateItem(form({ name: " 노트북 ", description: " " }));

    expect(state).toEqual({ ok: true });
    expect(JSON.parse((fetchMock.mock.calls[0][1] as RequestInit).body as string)).toEqual({
      name: "노트북",
    });
  });

  it("백엔드 오류 메시지를 입력값과 함께 돌려준다", async () => {
    mockFetch(Response.json({ status: 400, detail: "너무 깁니다" }, { status: 400 }));

    const state = await submitCreateItem(form({ name: "노트북", description: "" }));

    expect(state).toEqual({ error: "너무 깁니다", values: { name: "노트북", description: "" } });
  });
});

describe("submitUpdateItem", () => {
  it("잘못된 id는 백엔드를 부르지 않는다", async () => {
    const fetchMock = mockFetch(Response.json({}));

    const state = await submitUpdateItem(-1, form({ name: "노트북" }));

    expect(state.error).toBe("잘못된 요청입니다.");
    expect(fetchMock).not.toHaveBeenCalled();
  });

  it("PUT /api/items/{id} 로 수정한다", async () => {
    const fetchMock = mockFetch(Response.json({ id: 3 }));

    const state = await submitUpdateItem(3, form({ name: "새 이름", description: "설명" }));

    expect(state).toEqual({ ok: true });
    expect(fetchMock.mock.calls[0][0]).toBe("/api/items/3");
    expect((fetchMock.mock.calls[0][1] as RequestInit).method).toBe("PUT");
  });
});

describe("submitDeleteItem", () => {
  it("이미 지워진 항목(404)은 성공으로 본다", async () => {
    mockFetch(Response.json({ status: 404, detail: "없음" }, { status: 404 }));

    await expect(submitDeleteItem(3)).resolves.toEqual({ ok: true });
  });

  it("연결 실패는 오류로 돌려준다", async () => {
    mockFetch(new TypeError("Failed to fetch"));

    await expect(submitDeleteItem(3)).resolves.toEqual({
      error: "삭제하지 못했습니다. 백엔드에 연결할 수 없습니다.",
    });
  });
});
