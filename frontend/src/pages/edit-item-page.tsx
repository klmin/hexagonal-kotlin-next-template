import { useQuery } from "@tanstack/react-query";
import { useParams } from "react-router";

import { NotFoundMessage } from "@/components/not-found-message";
import { Card, CardContent } from "@/components/ui/card";
import { EditItemForm } from "@/features/items/edit-item-form";
import { ApiError } from "@/lib/api/client";
import { getItem, itemKeys, parseItemId } from "@/lib/api/items";

// /items/:id/edit
export function EditItemPage() {
  const id = parseItemId(useParams().id ?? null);
  return (
    <main className="mx-auto flex w-full max-w-xl flex-col gap-8 px-4 py-12">
      <h1 className="text-2xl font-semibold">항목 수정</h1>
      {/* 숫자가 아닌 id 는 백엔드를 부르지 않는다 */}
      {id === null ? <NotFoundMessage /> : <EditItem id={id} />}
    </main>
  );
}

function EditItem({ id }: { id: number }) {
  const { data, isPending, error } = useQuery({
    queryKey: itemKeys.detail(id),
    queryFn: ({ signal }) => getItem(id, signal),
  });

  if (isPending) {
    return (
      <p role="status" className="text-sm text-muted-foreground">
        불러오는 중...
      </p>
    );
  }
  if (error instanceof ApiError && error.status === 404) {
    return <NotFoundMessage />;
  }
  if (error) {
    return (
      <p className="text-sm text-destructive">
        항목을 불러오지 못했습니다. 백엔드가 실행 중인지 확인해 주세요.
      </p>
    );
  }
  return (
    <Card>
      <CardContent>
        {/* key: 다른 항목으로 바뀌면 폼 상태를 새로 시작한다 */}
        <EditItemForm key={data.id} item={data} />
      </CardContent>
    </Card>
  );
}
