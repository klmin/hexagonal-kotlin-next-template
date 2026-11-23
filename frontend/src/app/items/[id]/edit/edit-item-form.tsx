"use client";

import Link from "next/link";
import { useActionState, useId } from "react";

import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Textarea } from "@/components/ui/textarea";
import type { Item } from "@/lib/api/items";

import { updateItemAction, type UpdateItemState } from "../../actions";

const initialState: UpdateItemState = {};

export function EditItemForm({ item }: { item: Item }) {
  const [state, formAction, pending] = useActionState(
    updateItemAction.bind(null, item.id),
    initialState,
  );
  const id = useId();
  const errorId = `${id}-error`;

  return (
    <form action={formAction} className="flex flex-col gap-4">
      <div className="flex flex-col gap-2">
        <Label htmlFor={`${id}-name`}>이름</Label>
        <Input
          id={`${id}-name`}
          name="name"
          maxLength={100}
          required
          defaultValue={state.values?.name ?? item.name}
          aria-invalid={state.error ? true : undefined}
          aria-describedby={state.error ? errorId : undefined}
        />
      </div>
      <div className="flex flex-col gap-2">
        <Label htmlFor={`${id}-description`}>설명 (선택)</Label>
        <Textarea
          id={`${id}-description`}
          name="description"
          maxLength={500}
          rows={2}
          defaultValue={state.values?.description ?? item.description ?? ""}
        />
      </div>
      <div className="flex items-center gap-2">
        <Button type="submit" disabled={pending}>
          {pending ? "저장 중..." : "저장"}
        </Button>
        <Button variant="ghost" asChild>
          <Link href="/">취소</Link>
        </Button>
      </div>
      {state.error && (
        <p id={errorId} role="alert" className="text-sm text-destructive">
          {state.error}
        </p>
      )}
    </form>
  );
}
