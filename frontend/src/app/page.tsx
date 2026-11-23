import { Suspense } from "react";

import { Card, CardContent } from "@/components/ui/card";

import { CreateItemForm } from "./items/create-item-form";
import { ItemList } from "./items/item-list";

export default function Home({ searchParams }: PageProps<"/">) {
  return (
    <main className="mx-auto flex w-full max-w-xl flex-col gap-8 px-4 py-12">
      <header>
        <h1 className="text-2xl font-semibold">Items</h1>
        <p className="text-sm text-muted-foreground">
          Spring Boot <code>/api/items</code> 연동 샘플
        </p>
      </header>

      <Card>
        <CardContent>
          <CreateItemForm />
        </CardContent>
      </Card>

      <section>
        <Suspense
          fallback={
            <p role="status" className="text-sm text-muted-foreground">
              불러오는 중...
            </p>
          }
        >
          {/* searchParams 는 요청 시점 데이터라 Suspense 안의 ItemList 에서 await 한다 */}
          <ItemList searchParams={searchParams} />
        </Suspense>
      </section>
    </main>
  );
}
