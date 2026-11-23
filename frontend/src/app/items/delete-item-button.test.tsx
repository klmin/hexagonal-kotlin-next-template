import { render, screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { describe, expect, it, vi } from "vitest";

import { DeleteItemButton } from "./delete-item-button";

const deleteItemAction = vi.fn();

vi.mock("./actions", () => ({
  deleteItemAction: (...args: unknown[]) => deleteItemAction(...args),
}));

describe("DeleteItemButton", () => {
  it("삭제에 실패하면 목록을 유지한 채 오류를 보여 준다", async () => {
    deleteItemAction.mockResolvedValue({
      error: "삭제하지 못했습니다. 백엔드에 연결할 수 없습니다.",
    });
    render(<DeleteItemButton id={7} name="노트북" />);

    await userEvent.click(screen.getByRole("button", { name: "노트북 삭제" }));

    expect(await screen.findByRole("alert")).toHaveTextContent("삭제하지 못했습니다");
    expect(deleteItemAction.mock.calls[0][0]).toBe(7);
  });
});
