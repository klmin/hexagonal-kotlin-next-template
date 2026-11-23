import "server-only";

import { z } from "zod";

const schema = z.object({
  BACKEND_URL: z.url({ protocol: /^https?$/ }).default("http://localhost:8080"),
});

export type ServerEnv = z.infer<typeof schema>;

// 서버 전용 환경변수. 잘못된 값이면 어떤 변수가 문제인지 바로 알 수 있게 실패시킨다
export function serverEnv(source: Record<string, string | undefined> = process.env): ServerEnv {
  const result = schema.safeParse(source);
  if (!result.success) {
    const issues = result.error.issues.map((i) => `${i.path.join(".")}: ${i.message}`).join(", ");
    throw new Error(`환경변수 설정 오류 - ${issues}`);
  }
  return result.data;
}
