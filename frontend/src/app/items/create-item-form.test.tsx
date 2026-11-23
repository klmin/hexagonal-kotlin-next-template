import { render, screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { describe, expect, it, vi } from "vitest";

import { CreateItemForm } from "./create-item-form";

const createItemAction = vi.fn();

vi.mock("./actions", () => ({
  createItemAction: (...args: unknown[]) => createItemAction(...args),
}));

describe("CreateItemForm", () => {
  it("서버 액션이 돌려준 오류를 보여 주고 입력값을 유지한다", async () => {
    createItemAction.mockResolvedValue({
      error: "이미 있는 이름입니다.",
      values: { name: "노트북", description: "회사용" },
    });
    render(<CreateItemForm />);

    await userEvent.type(screen.getByLabelText("이름"), "노트북");
    await userEvent.type(screen.getByLabelText("설명 (선택)"), "회사용");
    await userEvent.click(screen.getByRole("button", { name: "추가" }));

    expect(await screen.findByRole("alert")).toHaveTextContent("이미 있는 이름입니다.");
    const formData = createItemAction.mock.calls[0][1] as FormData;
    expect(formData.get("name")).toBe("노트북");

    const name = screen.getByLabelText("이름");
    expect(name).toHaveValue("노트북");
    expect(screen.getByLabelText("설명 (선택)")).toHaveValue("회사용");
    expect(name).toHaveAttribute("aria-invalid", "true");
    expect(name).toHaveAccessibleDescription("이미 있는 이름입니다.");
  });

  it("성공하면 입력값을 비운다", async () => {
    createItemAction.mockResolvedValue({});
    render(<CreateItemForm />);

    await userEvent.type(screen.getByLabelText("이름"), "노트북");
    await userEvent.click(screen.getByRole("button", { name: "추가" }));

    await vi.waitFor(() => expect(screen.getByLabelText("이름")).toHaveValue(""));
    expect(screen.queryByRole("alert")).toBeNull();
  });
});
