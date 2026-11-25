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
  // 폼은 처음 받은 값으로만 채워진다(defaultValue). 그래서 화면을 연 뒤 서버에서 한 번 받기 전에는
  // (오프라인이라 요청이 멈춘 경우 포함) 캐시에 남은 예전 값으로 폼을 보여 주지 않는다.
  // 한 번 보여 준 뒤에는 다시 불러오더라도 폼을 그대로 둬서 입력이 사라지지 않게 하고,
  // 입력하는 동안에는 창 포커스, 재연결로 다시 불러오지 않는다
  const { data, error, isFetchedAfterMount } = useQuery({
    queryKey: itemKeys.detail(id),
    queryFn: ({ signal }) => getItem(id, signal),
    refetchOnMount: "always",
    refetchOnWindowFocus: false,
    refetchOnReconnect: false,
  });

  if (!isFetchedAfterMount || (!data && !error)) {
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
