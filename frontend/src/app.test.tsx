import { screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { afterEach, describe, expect, it, vi } from "vitest";

import { renderWithQuery } from "@/test/render";

import { App } from "./app";

afterEach(() => {
  vi.unstubAllGlobals();
});

describe("App 경로", () => {
  it("없는 경로는 찾을 수 없다고 보여 준다", () => {
    renderWithQuery(<App />, { route: "/no-such-page" });

    expect(screen.getByRole("heading", { name: "페이지를 찾을 수 없습니다" })).toBeInTheDocument();
  });

  it("수정 화면은 /api/items/{id} 로 항목을 불러와 폼에 채운다", async () => {
    const fetchMock = vi.fn().mockImplementation(async () =>
      Response.json({
        id: 3,
        name: "노트북",
        description: null,
        createdAt: "2026-11-25T00:00:00Z",
      }),
    );
    vi.stubGlobal("fetch", fetchMock);

    renderWithQuery(<App />, { route: "/items/3/edit" });

    expect(await screen.findByLabelText("이름")).toHaveValue("노트북");
    expect(fetchMock.mock.calls[0][0]).toBe("/api/items/3");
  });

  it("수정 화면에서 취소하면 목록으로 간다", async () => {
    const item = { id: 3, name: "노트북", description: null, createdAt: "2026-11-25T00:00:00Z" };
    vi.stubGlobal(
      "fetch",
      vi
        .fn()
        .mockImplementation(async (url: string) =>
          Response.json(url === "/api/items" ? { items: [item], nextCursor: null } : item),
        ),
    );
    renderWithQuery(<App />, { route: "/items/3/edit" });

    await userEvent.click(await screen.findByRole("link", { name: "취소" }));

    expect(screen.getByTestId("location").textContent).toBe("/");
    expect(await screen.findByRole("heading", { name: "Items" })).toBeInTheDocument();
  });

  it("수정 화면의 id가 숫자가 아니면 백엔드를 부르지 않는다", () => {
    const fetchMock = vi.fn();
    vi.stubGlobal("fetch", fetchMock);

    renderWithQuery(<App />, { route: "/items/1abc/edit" });

    expect(screen.getByRole("heading", { name: "페이지를 찾을 수 없습니다" })).toBeInTheDocument();
    expect(fetchMock).not.toHaveBeenCalled();
  });

  it("없는 항목(백엔드 404)은 찾을 수 없다고 보여 준다", async () => {
    vi.stubGlobal(
      "fetch",
      vi
        .fn()
        .mockImplementation(async () =>
          Response.json({ status: 404, detail: "item not found: 9" }, { status: 404 }),
        ),
    );

    renderWithQuery(<App />, { route: "/items/9/edit" });

    expect(
      await screen.findByRole("heading", { name: "페이지를 찾을 수 없습니다" }),
    ).toBeInTheDocument();
  });
});
