import { Route, Routes } from "react-router";

import { EditItemPage } from "@/pages/edit-item-page";
import { HomePage } from "@/pages/home-page";
import { NotFoundPage } from "@/pages/not-found-page";

// 화면 경로. nginx 는 /api 와 정적 파일이 아닌 모든 경로에 index.html 을 돌려주고, 여기서 화면을 고른다
export function App() {
  return (
    <Routes>
      <Route index element={<HomePage />} />
      <Route path="items/:id/edit" element={<EditItemPage />} />
      <Route path="*" element={<NotFoundPage />} />
    </Routes>
  );
}
