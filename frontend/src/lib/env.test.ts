import { describe, expect, it } from "vitest";

import { serverEnv } from "./env";

describe("serverEnv", () => {
  it("BACKEND_URL이 없으면 기본값을 쓴다", () => {
    expect(serverEnv({}).BACKEND_URL).toBe("http://localhost:8080");
  });

  it("URL 형식이 아니면 변수 이름을 담아 실패한다", () => {
    expect(() => serverEnv({ BACKEND_URL: "backend:8080" })).toThrow(/BACKEND_URL/);
  });
});
