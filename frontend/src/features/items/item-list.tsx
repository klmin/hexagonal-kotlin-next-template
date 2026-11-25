import { useQuery } from "@tanstack/react-query";
import { Link, useSearchParams } from "react-router";

import { Button } from "@/components/ui/button";
import { isCursor, itemKeys, listItems } from "@/lib/api/items";

import { DeleteItemButton } from "./delete-item-button";

export function ItemList() {
  const [searchParams] = useSearchParams();
  const cursor = searchParams.get("cursor");
  const current = isCursor(cursor) ? cursor : undefined;

  const { data, isPending, isError } = useQuery({
    queryKey: itemKeys.list(current),
    queryFn: ({ signal }) => listItems(current, signal),
  });

  if (isPending) {
    return (
      <p role="status" className="text-sm text-muted-foreground">
        불러오는 중...
      </p>
    );
  }

  if (isError) {
    return (
      <p className="text-sm text-destructive">
        목록을 불러오지 못했습니다. 백엔드가 실행 중인지 확인해 주세요.
      </p>
    );
  }

  const { items, nextCursor } = data;

  if (items.length === 0) {
    return (
      <div className="flex flex-col gap-3">
        <p className="text-sm text-muted-foreground">
          {current ? "더 이상 항목이 없습니다." : "아직 항목이 없습니다."}
        </p>
        {current && <PageLinks nextCursor={null} />}
      </div>
    );
  }

  return (
    <div className="flex flex-col gap-3">
      <ul className="divide-y">
        {items.map((item) => (
          <li key={item.id} className="flex items-start justify-between gap-4 py-3">
            <div>
              <p className="font-medium">{item.name}</p>
              {item.description && (
                <p className="text-sm text-muted-foreground">{item.description}</p>
              )}
            </div>
            <div className="flex shrink-0 items-start gap-1">
              <Button variant="ghost" size="sm" asChild>
                <Link to={`/items/${item.id}/edit`}>수정</Link>
              </Button>
              <DeleteItemButton id={item.id} name={item.name} />
            </div>
          </li>
        ))}
      </ul>
      <PageLinks nextCursor={nextCursor} isFirstPage={!current} />
    </div>
  );
}

// 커서 페이징은 "다음"만 알 수 있다. 앞으로 돌아갈 때는 브라우저 뒤로 가기나 "처음으로"를 쓴다
function PageLinks({
  nextCursor,
  isFirstPage = false,
}: {
  nextCursor: string | null;
  isFirstPage?: boolean;
}) {
  if (!nextCursor && isFirstPage) {
    return null;
  }
  return (
    <nav aria-label="페이지" className="flex gap-2">
      {!isFirstPage && (
        <Button variant="outline" size="sm" asChild>
          <Link to="/">처음으로</Link>
        </Button>
      )}
      {nextCursor && (
        <Button variant="outline" size="sm" asChild>
          <Link to={`/?${new URLSearchParams({ cursor: nextCursor })}`}>다음</Link>
        </Button>
      )}
    </nav>
  );
}
