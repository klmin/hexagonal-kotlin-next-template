"use client";

import { useEffect } from "react";

import "./globals.css";

// 앱 전체가 깨졌을 때 뜨는 화면이라 컴포넌트를 import 하지 않고 HTML 과 테마 색만 쓴다
// 루트 layout 에서 난 오류는 error.tsx 가 잡지 못한다. layout 을 대신하므로 html, body 를 직접 둔다
export default function GlobalError({ error, retry }: { error: unknown; retry: () => void }) {
  useEffect(() => {
    console.error(error);
  }, [error]);

  return (
    <html lang="ko">
      <body>
        <main className="mx-auto flex w-full max-w-xl flex-col gap-4 px-4 py-12">
          <h1 className="text-xl font-semibold">문제가 발생했습니다</h1>
          <p className="text-sm text-muted-foreground">잠시 후 다시 시도해 주세요.</p>
          <button
            type="button"
            onClick={() => retry()}
            className="self-start rounded-md bg-primary px-4 py-2 text-sm font-medium text-primary-foreground"
          >
            다시 시도
          </button>
        </main>
      </body>
    </html>
  );
}
