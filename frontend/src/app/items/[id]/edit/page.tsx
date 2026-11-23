import { notFound } from "next/navigation";
import { Suspense } from "react";

import { Card, CardContent } from "@/components/ui/card";
import { ApiError } from "@/lib/api/client";
import { getItem, type Item } from "@/lib/api/items";

import { EditItemForm } from "./edit-item-form";

// params 는 요청 시점 데이터라 Suspense 안에서 await 한다 (cacheComponents)
export default function EditItemPage({ params }: PageProps<"/items/[id]/edit">) {
  return (
    <main className="mx-auto flex w-full max-w-xl flex-col gap-8 px-4 py-12">
      <h1 className="text-2xl font-semibold">항목 수정</h1>
      <Suspense fallback={<p className="text-sm text-muted-foreground">불러오는 중...</p>}>
        <EditItem params={params} />
      </Suspense>
    </main>
  );
}

async function EditItem({ params }: { params: Promise<{ id: string }> }) {
  const { id } = await params;
  const itemId = Number(id);
  if (!Number.isSafeInteger(itemId) || itemId <= 0) {
    notFound();
  }

  const item = await loadItem(itemId);
  return (
    <Card>
      <CardContent>
        <EditItemForm item={item} />
      </CardContent>
    </Card>
  );
}

async function loadItem(id: number): Promise<Item> {
  try {
    return await getItem(id);
  } catch (e) {
    if (e instanceof ApiError && e.status === 404) {
      notFound();
    }
    throw e;
  }
}
