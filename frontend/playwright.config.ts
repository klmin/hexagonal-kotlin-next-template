import { defineConfig, devices } from "@playwright/test";

// 이미 떠 있는 앱을 대상으로 돈다.
//   로컬: 루트에서 docker compose up -d --build --wait (또는 bootRun + npm run dev)
//   CI: .github/workflows/e2e.yml
export default defineConfig({
  testDir: "./e2e",
  fullyParallel: true,
  forbidOnly: !!process.env.CI,
  retries: process.env.CI ? 1 : 0,
  reporter: process.env.CI ? [["list"], ["html", { open: "never" }]] : "list",
  use: {
    baseURL: process.env.E2E_BASE_URL ?? "http://localhost:3000",
    trace: "retain-on-failure",
  },
  projects: [{ name: "chromium", use: { ...devices["Desktop Chrome"] } }],
});
