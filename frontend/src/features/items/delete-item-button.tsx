import { useQueryClient } from "@tanstack/react-query";
import { useActionState } from "react";

import { Button } from "@/components/ui/button";
import { itemKeys } from "@/lib/api/items";

import { submitDeleteItem, type DeleteItemState } from "./actions";

const initialState: DeleteItemState = {};

// 실패해도 목록 화면을 유지하고 오류를 보여 준다
export function DeleteItemButton({ id, name }: { id: number; name: string }) {
  const queryClient = useQueryClient();
  const [state, formAction, pending] = useActionState(async () => {
    const result = await submitDeleteItem(id);
    if (result.ok) {
      await queryClient.invalidateQueries({ queryKey: itemKeys.all });
    }
    return result;
  }, initialState);

  return (
    <form action={formAction} className="flex flex-col items-end gap-1">
      <Button
        type="submit"
        variant="ghost"
        size="sm"
        disabled={pending}
        aria-label={`${name} 삭제`}
        className="text-destructive hover:text-destructive"
      >
        {pending ? "삭제 중..." : "삭제"}
      </Button>
      {state.error && (
        <p role="alert" className="text-xs text-destructive">
          {state.error}
        </p>
      )}
    </form>
  );
}
