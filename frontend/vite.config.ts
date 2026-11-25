import tailwindcss from "@tailwindcss/vite";
import react from "@vitejs/plugin-react";
import { defineConfig, loadEnv } from "vite";

export default defineConfig(({ mode }) => {
  // .env.local 의 DEV_BACKEND_URL 도 읽는다 (접두사 없는 변수라 브라우저 코드에는 들어가지 않는다)
  const env = loadEnv(mode, process.cwd(), "");
  const backendUrl = env.DEV_BACKEND_URL || "http://localhost:8080";

  // 개발 서버, vite preview 에서 /api 를 백엔드로 넘긴다. 배포에서는 nginx 가 같은 일을 한다 (nginx/default.conf.template)
  const proxy = { "/api": { target: backendUrl, changeOrigin: false } };

  return {
    plugins: [react(), tailwindcss()],
    resolve: { tsconfigPaths: true },
    server: { port: 3000, strictPort: true, proxy },
    preview: { port: 3000, strictPort: true, proxy },
    test: {
      environment: "jsdom",
      setupFiles: ["./src/test/setup.ts"],
      include: ["src/**/*.test.{ts,tsx}"],
    },
  };
});
