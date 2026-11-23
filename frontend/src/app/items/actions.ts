"use server";

import { refresh } from "next/cache";
import { redirect } from "next/navigation";

import { ApiError } from "@/lib/api/client";
import { createItem, deleteItem, isItemId, updateItem } from "@/lib/api/items";
import { logError } from "@/lib/log";

export type CreateItemState = {
  error?: string;
  // 실패했을 때 입력값을 다시 채우기 위한 값. 폼 액션이 끝나면 React가 폼을 비우기 때문
  values?: { name: string; description: string };
};

export async function createItemAction(
  _prev: CreateItemState,
  formData: FormData,
): Promise<CreateItemState> {
  const name = String(formData.get("name") ?? "").trim();
  const description = String(formData.get("description") ?? "").trim();
  const values = { name, description };

  if (!name) {
    return { error: "이름을 입력해 주세요.", values };
  }

  try {
    await createItem({ name, description: description || undefined });
  } catch (e) {
    if (e instanceof ApiError) {
      return { error: e.message, values };
    }
    logError("createItemAction failed", e);
    return { error: "백엔드에 연결할 수 없습니다.", values };
  }

  // 새 항목은 최신순 첫 페이지에 나온다. 다음 페이지(?cursor=)를 보고 있었어도 첫 페이지로 돌아가야 보인다
  redirect("/");
}

export type UpdateItemState = CreateItemState;

// 폼에서 bind 로 id 를 넘긴다: updateItemAction.bind(null, id)
export async function updateItemAction(
  id: number,
  _prev: UpdateItemState,
  formData: FormData,
): Promise<UpdateItemState> {
  const name = String(formData.get("name") ?? "").trim();
  const description = String(formData.get("description") ?? "").trim();
  const values = { name, description };

  if (!isItemId(id)) {
    return { error: "잘못된 요청입니다.", values };
  }
  if (!name) {
    return { error: "이름을 입력해 주세요.", values };
  }

  try {
    await updateItem(id, { name, description: description || undefined });
  } catch (e) {
    if (e instanceof ApiError) {
      return { error: e.message, values };
    }
    logError("updateItemAction failed", e);
    return { error: "백엔드에 연결할 수 없습니다.", values };
  }

  // redirect 는 예외로 동작하므로 try 밖에서 부른다
  redirect("/");
}

export type DeleteItemState = {
  error?: string;
};

// 실패해도 목록 화면을 유지하고 오류를 상태로 돌려준다 (deleteItemAction.bind(null, id))
export async function deleteItemAction(
  id: number,
  _prev: DeleteItemState,
): Promise<DeleteItemState> {
  if (!isItemId(id)) {
    return { error: "잘못된 요청입니다." };
  }

  try {
    await deleteItem(id);
  } catch (e) {
    // 이미 지워졌으면 성공으로 보고 목록만 새로 그린다
    if (!(e instanceof ApiError && e.status === 404)) {
      if (e instanceof ApiError) {
        return { error: e.message };
      }
      logError("deleteItemAction failed", e);
      return { error: "삭제하지 못했습니다. 백엔드에 연결할 수 없습니다." };
    }
  }

  refresh();
  return {};
}
