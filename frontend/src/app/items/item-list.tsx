import Link from "next/link";
import { unstable_rethrow } from "next/navigation";

import { Button } from "@/components/ui/button";
import { isCursor, listItems, type ItemPage } from "@/lib/api/items";
import { logError } from "@/lib/log";

import { DeleteItemButton } from "./delete-item-button";

// cacheComponents 환경이라 <Suspense> 안에서 렌더링해야 함
export async function ItemList({
  searchParams,
}: {
  searchParams: Promise<Record<string, string | string[] | undefined>>;
}) {
  const { cursor } = await searchParams;
  const current = isCursor(cursor) ? cursor : undefined;

  let page: ItemPage;
  try {
    page = await listItems(current);
  } catch (err) {
    // try 안에서 notFound(), redirect() 등을 쓰면 Next.js 가 처리하게 다시 던진다
    unstable_rethrow(err);
    logError("ItemList failed", err);
    return (
      <p className="text-sm text-destructive">
        목록을 불러오지 못했습니다. 백엔드가 실행 중인지 확인해 주세요.
      </p>
    );
  }

  const { items, nextCursor } = page;

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
                <Link href={`/items/${item.id}/edit`}>수정</Link>
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
          <Link href="/">처음으로</Link>
        </Button>
      )}
      {nextCursor && (
        <Button variant="outline" size="sm" asChild>
          <Link href={`/?${new URLSearchParams({ cursor: nextCursor })}`}>다음</Link>
        </Button>
      )}
    </nav>
  );
}
