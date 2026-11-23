"use client";

import { useActionState } from "react";

import { Button } from "@/components/ui/button";

import { deleteItemAction, type DeleteItemState } from "./actions";

const initialState: DeleteItemState = {};

export function DeleteItemButton({ id, name }: { id: number; name: string }) {
  const [state, formAction, pending] = useActionState(
    deleteItemAction.bind(null, id),
    initialState,
  );

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
