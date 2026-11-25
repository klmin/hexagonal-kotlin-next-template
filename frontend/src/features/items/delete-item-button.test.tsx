import { screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { beforeEach, describe, expect, it, vi } from "vitest";

import { renderWithQuery } from "@/test/render";

import { DeleteItemButton } from "./delete-item-button";

const submitDeleteItem = vi.fn();

vi.mock("./actions", () => ({
  submitDeleteItem: (...args: unknown[]) => submitDeleteItem(...args),
}));

beforeEach(() => {
  submitDeleteItem.mockReset();
});

describe("DeleteItemButton", () => {
  it("삭제에 실패하면 목록을 유지한 채 오류를 보여 준다", async () => {
    submitDeleteItem.mockResolvedValue({
      error: "삭제하지 못했습니다. 백엔드에 연결할 수 없습니다.",
    });
    renderWithQuery(<DeleteItemButton id={7} name="노트북" />);

    await userEvent.click(screen.getByRole("button", { name: "노트북 삭제" }));

    expect(await screen.findByRole("alert")).toHaveTextContent("삭제하지 못했습니다");
    expect(submitDeleteItem).toHaveBeenCalledWith(7);
  });

  it("삭제에 성공하면 목록을 다시 불러온다", async () => {
    submitDeleteItem.mockResolvedValue({ ok: true });
    const { queryClient } = renderWithQuery(<DeleteItemButton id={7} name="노트북" />);
    const invalidate = vi.spyOn(queryClient, "invalidateQueries");

    await userEvent.click(screen.getByRole("button", { name: "노트북 삭제" }));

    await vi.waitFor(() => expect(invalidate).toHaveBeenCalledWith({ queryKey: ["items"] }));
    expect(screen.queryByRole("alert")).toBeNull();
  });
});
