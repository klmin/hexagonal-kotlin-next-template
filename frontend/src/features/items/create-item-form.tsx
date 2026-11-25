import { useQueryClient } from "@tanstack/react-query";
import { useActionState, useId } from "react";
import { useNavigate, useSearchParams } from "react-router";

import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Textarea } from "@/components/ui/textarea";
import { itemKeys } from "@/lib/api/items";

import { submitCreateItem, type ItemFormState } from "./actions";

const initialState: ItemFormState = {};

export function CreateItemForm() {
  const queryClient = useQueryClient();
  const navigate = useNavigate();
  const [searchParams] = useSearchParams();

  const [state, formAction, pending] = useActionState(
    async (_prev: ItemFormState, formData: FormData) => {
      const result = await submitCreateItem(formData);
      if (result.ok) {
        await queryClient.invalidateQueries({ queryKey: itemKeys.all });
        // 새 항목은 최신순 첫 페이지에 나온다. 다음 페이지(?cursor=)를 보고 있었으면 첫 페이지로 돌아가야 보인다
        if (searchParams.has("cursor")) {
          navigate("/");
        }
      }
      return result;
    },
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
