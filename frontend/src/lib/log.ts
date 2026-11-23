import "server-only";

// 서버 로그에 오류를 남긴다. 오류 메시지에 요청 값이 섞여 있을 수 있어서
// 줄바꿈을 지워 로그 한 줄을 위조하지 못하게 한다 (log injection)
export function logError(context: string, error: unknown): void {
  const message = error instanceof Error ? `${error.name}: ${error.message}` : String(error);
  console.error(`${context}: ${message.replace(/\n|\r/g, "")}`);
}
