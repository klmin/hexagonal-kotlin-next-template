import { expect, test } from "@playwright/test";

// 모든 테스트가 같은 백엔드 데이터를 쓴다. 페이징 테스트가 항목을 여러 개 만들면 병렬로 도는 다른 테스트의 항목이
// 첫 페이지 밖으로 밀릴 수 있어서, 이 파일은 순서대로 실행한다 (실패해도 다음 테스트는 계속 돈다)
test.describe.configure({ mode: "default" });

test("항목을 추가하면 목록에 보인다", async ({ page }) => {
  const name = `e2e-${Date.now()}`;

  await page.goto("/");
  await page.getByLabel("이름").fill(name);
  await page.getByLabel("설명 (선택)").fill("playwright");
  await page.getByRole("button", { name: "추가" }).click();

  await expect(page.getByText(name)).toBeVisible();

  // 새로고침해도 남아 있으면 백엔드에 저장된 것
  await page.reload();
  await expect(page.getByText(name)).toBeVisible();
});

test("이름이 공백뿐이면 오류를 보여 준다", async ({ page }) => {
  await page.goto("/");
  await page.getByLabel("이름").fill("   ");
  await page.getByRole("button", { name: "추가" }).click();

  await expect(page.locator("form").getByRole("alert")).toHaveText("이름을 입력해 주세요.");
});

// SPA 라서 nginx 는 없는 화면 경로에도 index.html(200)을 주고, 찾을 수 없다는 건 화면이 보여 준다
test("없는 페이지는 찾을 수 없다고 보여 준다", async ({ page }) => {
  await page.goto("/no-such-page");

  await expect(page.getByText("페이지를 찾을 수 없습니다")).toBeVisible();
});

test("항목을 수정하면 목록에 바뀐 이름이 보인다", async ({ page }) => {
  const name = `e2e-edit-${Date.now()}`;
  const renamed = `${name}-renamed`;

  await page.goto("/");
  await page.getByLabel("이름").fill(name);
  await page.getByRole("button", { name: "추가" }).click();

  const row = page.getByRole("listitem").filter({ hasText: name });
  await row.getByRole("link", { name: "수정" }).click();

  const editPage = page.getByRole("main").filter({
    has: page.getByRole("heading", { name: "항목 수정" }),
  });
  await expect(page).toHaveURL(/\/items\/\d+\/edit$/);
  await expect(editPage).toBeVisible();
  // 주소로 바로 들어와도(새로고침) 같은 화면이 나와야 한다 (nginx 가 index.html 을 준다)
  await page.reload();
  await expect(editPage.getByLabel("이름")).toHaveValue(name);
  await editPage.getByLabel("이름").fill(renamed);
  await editPage.getByRole("button", { name: "저장" }).click();

  await expect(page).toHaveURL("/");
  await expect(page.getByText(renamed)).toBeVisible();
});

test("항목을 삭제하면 목록에서 사라진다", async ({ page }) => {
  const name = `e2e-delete-${Date.now()}`;

  await page.goto("/");
  await page.getByLabel("이름").fill(name);
  await page.getByRole("button", { name: "추가" }).click();
  await expect(page.getByText(name)).toBeVisible();

  await page
    .getByRole("listitem")
    .filter({ hasText: name })
    .getByRole("button", { name: "삭제" })
    .click();

  await expect(page.getByText(name)).toHaveCount(0);
  await page.reload();
  await expect(page.getByText(name)).toHaveCount(0);
});

test("없는 항목의 수정 화면은 찾을 수 없다고 보여 준다", async ({ page }) => {
  await page.goto("/items/999999999/edit");

  await expect(page.getByText("페이지를 찾을 수 없습니다")).toBeVisible();
});

test("수정 화면의 id가 숫자가 아니면 백엔드를 부르지 않는다", async ({ page }) => {
  const apiCalls: string[] = [];
  page.on("request", (req) => {
    if (new URL(req.url()).pathname.startsWith("/api/")) apiCalls.push(req.url());
  });

  await page.goto("/items/1%2F..%2F..%2Factuator/edit");

  await expect(page.getByText("페이지를 찾을 수 없습니다")).toBeVisible();
  expect(apiCalls).toEqual([]);
});

test("목록은 20개씩 나오고 다음 페이지로 이어진다", async ({ page, request }) => {
  // 화면과 같은 오리진의 /api 로 만든다 (nginx 또는 개발 서버가 백엔드로 넘긴다)
  const prefix = `e2e-page-${Date.now()}`;
  for (let i = 1; i <= 21; i++) {
    const res = await request.post("/api/items", {
      data: { name: `${prefix}-${String(i).padStart(2, "0")}` },
    });
    expect(res.status()).toBe(201);
  }

  await page.goto("/");
  // 최신순이라 가장 나중에 만든 21번이 첫 페이지 맨 위, 1번은 다음 페이지
  await expect(page.getByText(`${prefix}-21`)).toBeVisible();
  await expect(page.getByText(`${prefix}-01`)).toHaveCount(0);

  await page
    .getByRole("navigation", { name: "페이지" })
    .getByRole("link", { name: "다음" })
    .click();

  await expect(page).toHaveURL(/\?cursor=/);
  await expect(page.getByText(`${prefix}-01`)).toBeVisible();
  await expect(page.getByText(`${prefix}-21`)).toHaveCount(0);
  await expect(page.getByRole("link", { name: "처음으로" })).toBeVisible();

  // 다음 페이지에서 추가해도 새 항목이 보이도록 첫 페이지로 돌아간다
  const created = `${prefix}-new`;
  await page.getByLabel("이름").fill(created);
  await page.getByRole("button", { name: "추가" }).click();
  await expect(page).not.toHaveURL(/cursor=/);
  await expect(page.getByText(created)).toBeVisible();
});

test("잘못된 커서는 첫 페이지로 보여 준다", async ({ page }) => {
  await page.goto("/?cursor=../../actuator");

  await expect(page.getByRole("heading", { name: "Items" })).toBeVisible();
  await expect(page.getByText("목록을 불러오지 못했습니다")).toHaveCount(0);
});
