import { Card, CardContent } from "@/components/ui/card";
import { CreateItemForm } from "@/features/items/create-item-form";
import { ItemList } from "@/features/items/item-list";

export function HomePage() {
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
        <ItemList />
      </section>
    </main>
  );
}
