import { NotFoundMessage } from "@/components/not-found-message";

export function NotFoundPage() {
  return (
    <main className="mx-auto flex w-full max-w-xl flex-col gap-4 px-4 py-12">
      <NotFoundMessage />
    </main>
  );
}
