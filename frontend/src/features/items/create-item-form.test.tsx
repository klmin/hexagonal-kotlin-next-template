import { screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { beforeEach, describe, expect, it, vi } from "vitest";

import { renderWithQuery } from "@/test/render";

import { CreateItemForm } from "./create-item-form";

const submitCreateItem = vi.fn();

vi.mock("./actions", () => ({
  submitCreateItem: (...args: unknown[]) => submitCreateItem(...args),
}));

beforeEach(() => {
  submitCreateItem.mockReset();
});

describe("CreateItemForm", () => {
  it("실패하면 오류를 보여 주고 입력값을 유지한다", async () => {
    submitCreateItem.mockResolvedValue({
      error: "이미 있는 이름입니다.",
      values: { name: "노트북", description: "회사용" },
    });
    renderWithQuery(<CreateItemForm />);

    await userEvent.type(screen.getByLabelText("이름"), "노트북");
    await userEvent.type(screen.getByLabelText("설명 (선택)"), "회사용");
    await userEvent.click(screen.getByRole("button", { name: "추가" }));

    expect(await screen.findByRole("alert")).toHaveTextContent("이미 있는 이름입니다.");
    const formData = submitCreateItem.mock.calls[0][0] as FormData;
    expect(formData.get("name")).toBe("노트북");

    const name = screen.getByLabelText("이름");
    expect(name).toHaveValue("노트북");
    expect(screen.getByLabelText("설명 (선택)")).toHaveValue("회사용");
    expect(name).toHaveAttribute("aria-invalid", "true");
    expect(name).toHaveAccessibleDescription("이미 있는 이름입니다.");
  });

  it("성공하면 입력값을 비우고 목록을 다시 불러온다", async () => {
    submitCreateItem.mockResolvedValue({ ok: true });
    const { queryClient } = renderWithQuery(<CreateItemForm />);
    const invalidate = vi.spyOn(queryClient, "invalidateQueries");

    await userEvent.type(screen.getByLabelText("이름"), "노트북");
    await userEvent.click(screen.getByRole("button", { name: "추가" }));

    await vi.waitFor(() => expect(screen.getByLabelText("이름")).toHaveValue(""));
    expect(screen.queryByRole("alert")).toBeNull();
    expect(invalidate).toHaveBeenCalledWith({ queryKey: ["items"] });
    // 첫 페이지에서는 이동하지 않는다
    expect(screen.getByTestId("location").textContent).toBe("/");
  });

  it("다음 페이지에서 추가하면 첫 페이지로 이동한다", async () => {
    submitCreateItem.mockResolvedValue({ ok: true });
    renderWithQuery(<CreateItemForm />, { route: "/?cursor=abc" });

    await userEvent.type(screen.getByLabelText("이름"), "노트북");
    await userEvent.click(screen.getByRole("button", { name: "추가" }));

    await vi.waitFor(() => expect(screen.getByTestId("location").textContent).toBe("/"));
  });
});
