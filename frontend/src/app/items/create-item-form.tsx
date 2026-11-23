"use client";

import { useActionState, useId } from "react";

import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Textarea } from "@/components/ui/textarea";

import { createItemAction, type CreateItemState } from "./actions";

const initialState: CreateItemState = {};

export function CreateItemForm() {
  const [state, formAction, pending] = useActionState(createItemAction, initialState);
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
          defaultValue={state.values?.name}
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
          defaultValue={state.values?.description}
        />
      </div>
      <Button type="submit" disabled={pending} className="self-start">
        {pending ? "추가 중..." : "추가"}
      </Button>
      {state.error && (
        <p id={errorId} role="alert" className="text-sm text-destructive">
          {state.error}
        </p>
      )}
    </form>
  );
}
