import "@testing-library/jest-dom/vitest";

import { cleanup } from "@testing-library/react";
import { afterEach } from "vitest";

// vitest globals 를 쓰지 않아서 Testing Library 자동 정리가 동작하지 않는다. 테스트마다 렌더링한 화면을 지운다
afterEach(() => {
  cleanup();
});
