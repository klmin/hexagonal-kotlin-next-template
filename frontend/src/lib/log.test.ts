import { afterEach, describe, expect, it, vi } from "vitest";

import { logError } from "./log";

afterEach(() => {
  vi.restoreAllMocks();
});

describe("logError", () => {
  it("줄바꿈을 지워서 한 줄로 남긴다", () => {
    const spy = vi.spyOn(console, "error").mockImplementation(() => {});

    logError("deleteItemAction failed", new Error("invalid item id: 1\nINFO fake log line"));

    expect(spy).toHaveBeenCalledWith(
      "deleteItemAction failed: Error: invalid item id: 1INFO fake log line",
    );
  });
});
