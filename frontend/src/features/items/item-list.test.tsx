import { screen } from "@testing-library/react";
import { afterEach, describe, expect, it, vi } from "vitest";

import { renderWithQuery } from "@/test/render";

import { ItemList } from "./item-list";

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

describe("ItemList", () => {
  it("항목과 수정 링크, 다음 페이지 링크를 보여 준다", async () => {
    mockFetch(
      Response.json({
        items: [
          { id: 3, name: "노트북", description: "회사용", createdAt: "2026-11-25T00:00:00Z" },
        ],
        nextCursor: "next_1",
      }),
    );
    renderWithQuery(<ItemList />);

    expect(await screen.findByText("노트북")).toBeInTheDocument();
    expect(screen.getByText("회사용")).toBeInTheDocument();
    expect(screen.getByRole("link", { name: "수정" })).toHaveAttribute("href", "/items/3/edit");
    expect(screen.getByRole("link", { name: "다음" })).toHaveAttribute("href", "/?cursor=next_1");
    expect(screen.queryByRole("link", { name: "처음으로" })).toBeNull();
  });

  it("다음 페이지에서는 커서로 조회하고 처음으로 링크를 보여 준다", async () => {
    const fetchMock = mockFetch(Response.json({ items: [], nextCursor: null }));
    renderWithQuery(<ItemList />, { route: "/?cursor=next_1" });

    expect(await screen.findByText("더 이상 항목이 없습니다.")).toBeInTheDocument();
    expect(fetchMock.mock.calls[0][0]).toBe("/api/items?cursor=next_1");
    expect(screen.getByRole("link", { name: "처음으로" })).toHaveAttribute("href", "/");
  });

  it("잘못된 커서는 무시하고 첫 페이지를 조회한다", async () => {
    const fetchMock = mockFetch(Response.json({ items: [], nextCursor: null }));
    renderWithQuery(<ItemList />, { route: "/?cursor=..%2F..%2Factuator" });

    expect(await screen.findByText("아직 항목이 없습니다.")).toBeInTheDocument();
    expect(fetchMock.mock.calls[0][0]).toBe("/api/items");
  });

  it("백엔드에 연결하지 못하면 오류를 보여 준다", async () => {
    mockFetch(new TypeError("Failed to fetch"));
    renderWithQuery(<ItemList />);

    expect(await screen.findByText(/목록을 불러오지 못했습니다/)).toBeInTheDocument();
  });
});
