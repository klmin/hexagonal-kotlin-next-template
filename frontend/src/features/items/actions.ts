// 폼 제출 처리 (브라우저에서 실행). 컴포넌트가 useActionState 로 연결하고, 성공하면 쿼리를 무효화한다.
// 예상 가능한 오류(입력 오류, 백엔드 오류 응답)는 throw 하지 않고 상태로 돌려준다

import { ApiError, errorMessage } from "@/lib/api/client";
import { createItem, deleteItem, isItemId, updateItem } from "@/lib/api/items";

export type ItemFormState = {
  ok?: boolean;
  error?: string;
  // 실패했을 때 입력값을 다시 채우기 위한 값. 폼 액션이 끝나면 React가 폼을 비우기 때문
  values?: { name: string; description: string };
};

function readForm(formData: FormData) {
  return {
    name: String(formData.get("name") ?? "").trim(),
    description: String(formData.get("description") ?? "").trim(),
  };
}

export async function submitCreateItem(formData: FormData): Promise<ItemFormState> {
  const values = readForm(formData);
  if (!values.name) {
    return { error: "이름을 입력해 주세요.", values };
  }

  try {
    await createItem({ name: values.name, description: values.description || undefined });
  } catch (e) {
    return { error: errorMessage(e), values };
  }
  return { ok: true };
}

export async function submitUpdateItem(id: number, formData: FormData): Promise<ItemFormState> {
  const values = readForm(formData);
  if (!isItemId(id)) {
    return { error: "잘못된 요청입니다.", values };
  }
  if (!values.name) {
    return { error: "이름을 입력해 주세요.", values };
  }

  try {
    await updateItem(id, { name: values.name, description: values.description || undefined });
  } catch (e) {
    return { error: errorMessage(e), values };
  }
  return { ok: true };
}

export type DeleteItemState = {
  ok?: boolean;
  error?: string;
};

export async function submitDeleteItem(id: number): Promise<DeleteItemState> {
  if (!isItemId(id)) {
    return { error: "잘못된 요청입니다." };
  }

  try {
    await deleteItem(id);
  } catch (e) {
    // 이미 지워졌으면 성공으로 보고 목록만 새로 그린다
    if (!(e instanceof ApiError && e.status === 404)) {
      return { error: errorMessage(e, "삭제하지 못했습니다. 백엔드에 연결할 수 없습니다.") };
    }
  }
  return { ok: true };
}
