import { Link } from "react-router";

import { Button } from "@/components/ui/button";

// 없는 경로와 없는 항목(백엔드 404)에서 같이 쓴다
export function NotFoundMessage() {
  return (
    <div className="flex flex-col gap-4">
      <h2 className="text-xl font-semibold">페이지를 찾을 수 없습니다</h2>
      <Button variant="outline" asChild className="self-start">
        <Link to="/">처음으로</Link>
      </Button>
    </div>
  );
}
