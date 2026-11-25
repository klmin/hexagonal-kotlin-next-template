import { request } from "./client";
import type { components } from "./schema";

// 백엔드 openapi/openapi.json 에서 생성한 타입 (npm run api:types)
export type Item = components["schemas"]["ItemResponse"];
export type ItemPage = components["schemas"]["ItemListResponse"];
export type CreateItemInput = components["schemas"]["CreateItemRequest"];
export type UpdateItemInput = components["schemas"]["UpdateItemRequest"];

// TanStack Query 키. 변경 후 itemKeys.all 로 목록과 상세를 함께 무효화한다
export const itemKeys = {
  all: ["items"] as const,
  list: (cursor?: string) => ["items", "list", cursor ?? null] as const,
  detail: (id: number) => ["items", "detail", id] as const,
};

// 백엔드 커서는 Base64URL 문자열이다. URL에서 받은 값이 다른 형태면 첫 페이지로 본다
export function isCursor(value: unknown): value is string {
  return typeof value === "string" && /^[A-Za-z0-9_-]{1,64}$/.test(value);
}

// 커서 페이징 목록 (최신순). 다음 페이지는 응답의 nextCursor 를 넘긴다
export function listItems(cursor?: string, signal?: AbortSignal): Promise<ItemPage> {
  const query = isCursor(cursor) ? `?${new URLSearchParams({ cursor })}` : "";
  return request<ItemPage>(`/api/items${query}`, { signal });
}

// id 는 URL(/items/:id/edit)에서 오므로 그대로 믿지 않는다.
// 양의 정수가 아니면 막아서 /api/items/../.. 같은 경로로 백엔드의 다른 API를 부르지 못하게 한다
export function isItemId(value: unknown): value is number {
  return typeof value === "number" && Number.isSafeInteger(value) && value > 0;
}

// URL 문자열을 항목 id 로 바꾼다. "12" 처럼 숫자만 있는 경우만 허용하고 아니면 null
export function parseItemId(value: string | null): number | null {
  if (!value || !/^[1-9][0-9]{0,15}$/.test(value)) {
    return null;
  }
  const id = Number(value);
  return isItemId(id) ? id : null;
}

function itemPath(id: number): string {
  if (!isItemId(id)) {
    throw new Error(`invalid item id: ${String(id)}`);
  }
  return `/api/items/${encodeURIComponent(String(id))}`;
}

export function getItem(id: number, signal?: AbortSignal): Promise<Item> {
  return request<Item>(itemPath(id), { signal });
}

export function updateItem(id: number, input: UpdateItemInput): Promise<Item> {
  return request<Item>(itemPath(id), {
    method: "PUT",
    body: JSON.stringify(input),
  });
}

export function deleteItem(id: number): Promise<void> {
  return request<void>(itemPath(id), { method: "DELETE" });
}

export function createItem(input: CreateItemInput): Promise<Item> {
  return request<Item>("/api/items", {
    method: "POST",
    body: JSON.stringify(input),
  });
}
