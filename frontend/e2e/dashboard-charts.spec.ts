import { test, expect, type Page } from "@playwright/test";
import AxeBuilder from "@axe-core/playwright";

const apiURL = process.env.E2E_API_URL ?? "http://localhost:8080";
if (!["localhost", "127.0.0.1", "[::1]"].includes(new URL(apiURL).hostname)) {
  throw new Error("Chart tests require a local disposable API");
}
const chartPanel = (page: Page) => page.getByRole("region", { name: /(?:Despesas|Receitas) por/ });
async function period(page: Page, start = "01/10/2026", end = "03/10/2026") {
  await expect(page.locator("#dashboard-start")).toBeEnabled();
  await page.locator("#dashboard-start").fill(start);
  await page.locator("#dashboard-end").fill(end);
  await page.getByRole("button", { name: "Aplicar período", exact: true }).click();
  await expect(page.locator("#dashboard-start")).toBeEnabled();
}
async function mockSummary(page: Page) {
  await page.route("**/dashboard/summary?*", route => route.fulfill({ json: { totalIncome: "100.00", totalExpense: "30.30", balance: "69.70" } }));
}
function summary(url: string) {
  const query = new URL(url).searchParams;
  const type = query.get("type");
  const groupBy = query.get("groupBy");
  const total = type === "INCOME" ? "100.00" : "30.30";
  const items = groupBy === "CATEGORY" ? [{ key: "42", label: "Alimentação", total }] : groupBy === "DAY" ? [
    { key: "2026-10-01", label: "01/10/2026", total },
    { key: "2026-10-02", label: "02/10/2026", total: "0.00" },
    { key: "2026-10-03", label: "03/10/2026", total: "0.00" },
  ] : [{ key: "2026-10", label: "outubro/2026", total }];
  return { type, groupBy, total, items };
}

for (const theme of ["light", "dark"] as const) {
  for (const width of [390, 1280]) {
    test(`${theme} ${width}: filtros, gráficos, período, acessibilidade e BRL`, async ({ page }) => {
      await page.setViewportSize({ width, height: 1000 });
      await page.emulateMedia({ colorScheme: theme });
      await mockSummary(page);
      await page.route("**/dashboard/transactions-summary?*", route => route.fulfill({ json: summary(route.request().url()) }));
      await page.goto("/dashboard");
      await period(page);
      const panel = chartPanel(page);
      await expect(panel.locator("[data-chart]")).toHaveAttribute("data-chart", "PIE");
      await expect(panel.getByRole("list")).toContainText("30,30");
      await expect(panel.getByRole("list")).toContainText("100%");
      await expect(page.locator('#chart-visual option[value="LINE"]')).toBeDisabled();
      await page.getByLabel("Tipo de gráfico", { exact: true }).selectOption("BAR");
      await expect(panel.locator("[data-chart]")).toHaveAttribute("data-chart", "BAR");
      await page.getByLabel("Movimentação", { exact: true }).selectOption("INCOME");
      await expect(panel.getByRole("heading")).toHaveText("Receitas por categoria");
      await expect(panel.getByRole("list")).toContainText("100,00");
      await page.getByLabel("Agrupamento", { exact: true }).selectOption("DAY");
      await page.getByLabel("Tipo de gráfico", { exact: true }).selectOption("LINE");
      await expect(panel.locator("[data-chart]")).toHaveAttribute("data-chart", "LINE");
      await expect(panel.getByRole("list")).toContainText("02/10/2026");
      await expect(panel.getByRole("list")).toContainText("0,00");
      await page.getByLabel("Tipo de gráfico", { exact: true }).selectOption("PIE");
      await expect(panel.locator("[data-chart]")).toHaveAttribute("data-chart", "PIE");
      await page.getByLabel("Agrupamento", { exact: true }).selectOption("MONTH");
      await expect(panel.getByRole("heading")).toHaveText("Receitas por mês");
      await expect(panel.locator("[data-chart]")).toHaveAttribute("data-chart", "LINE");
      await expect(page.locator("#dashboard-start")).toHaveValue("01/10/2026");
      await page.getByRole("button", { name: "Últimos 12 meses", exact: true }).click();
      await expect(page.locator('#chart-visual option[value="PIE"]')).toBeDisabled();
      await page.getByLabel("Agrupamento", { exact: true }).selectOption("CATEGORY");
      await expect(page.getByLabel("Tipo de gráfico", { exact: true })).toHaveValue("BAR");
      await expect(page.getByLabel("Movimentação", { exact: true })).toHaveValue("INCOME");
      await expect(panel.getByRole("list")).toBeVisible();
      expect(await page.evaluate(() => document.documentElement.scrollWidth <= window.innerWidth)).toBe(true);
      expect((await new AxeBuilder({ page }).include("section[aria-labelledby='financial-chart-title']").analyze()).violations).toEqual([]);
      await period(page);
      await page.getByLabel("Movimentação", { exact: true }).selectOption("EXPENSE");
      await page.getByLabel("Tipo de gráfico", { exact: true }).selectOption("PIE");
      await expect(panel.getByRole("list")).toContainText("30,30");
      await page.screenshot({ path: `test-results/charts-${theme}-${width}.png`, fullPage: true });
      await page.getByLabel("Agrupamento", { exact: true }).selectOption("DAY");
      await page.getByLabel("Tipo de gráfico", { exact: true }).selectOption("PIE");
      await period(page, "01/10/2026", "31/10/2026");
      await expect(panel.locator("[data-chart]")).toHaveAttribute("data-chart", "BAR");
      await expect(panel.getByRole("status")).toContainText("Gráfico ajustado para coluna");
      await expect(page.getByLabel("Movimentação", { exact: true })).toHaveValue("EXPENSE");
    });
  }
}

test("loading, erro, retry, vazio, 403 e proteção contra respostas antigas", async ({ page }) => {
  await mockSummary(page);
  let mode = "loading";
  let release: () => void = () => {};
  const held = new Promise<void>(resolve => { release = resolve; });
  await page.route("**/dashboard/transactions-summary?*", async route => {
    if (mode === "loading") { await held; await route.fulfill({ json: summary(route.request().url()) }).catch(() => {}); return; }
    if (mode === "error") { await route.fulfill({ status: 503, json: { message: "Falha temporária" } }); return; }
    if (mode === "forbidden") { await route.fulfill({ status: 403, json: { message: "Acesso negado" } }); return; }
    if (mode === "empty") { await route.fulfill({ json: { ...summary(route.request().url()), total: "0.00", items: [] } }); return; }
    await route.fulfill({ json: summary(route.request().url()) });
  });
  await page.goto("/dashboard");
  await expect(chartPanel(page).getByText("Carregando gráfico...")).toBeVisible();
  mode = "error";
  await page.getByLabel("Movimentação", { exact: true }).selectOption("INCOME");
  await expect(chartPanel(page).getByRole("alert")).toHaveText("Falha temporária");
  mode = "success";
  await chartPanel(page).getByRole("button", { name: "Tentar novamente" }).click();
  await expect(chartPanel(page).getByRole("list")).toContainText("100,00");
  release();
  await expect(chartPanel(page).getByRole("heading")).toHaveText("Receitas por categoria");
  await expect(chartPanel(page).getByRole("list")).toContainText("100,00");
  mode = "empty";
  await page.getByLabel("Movimentação", { exact: true }).selectOption("EXPENSE");
  await expect(chartPanel(page).getByRole("status")).toHaveText("Nenhuma movimentação encontrada para o período selecionado.");
  mode = "forbidden";
  await page.getByLabel("Movimentação", { exact: true }).selectOption("INCOME");
  await expect(chartPanel(page).getByRole("alert")).toHaveText("Acesso negado");
});

test("valida intervalos e limita o agrupamento diário sem perder filtros", async ({ page }) => {
  await mockSummary(page);
  await page.route("**/dashboard/transactions-summary?*", route => route.fulfill({ json: summary(route.request().url()) }));
  await page.goto("/dashboard");
  await period(page, "01/10/2026", "03/10/2026");
  await page.locator("#dashboard-start").fill("04/10/2026");
  await page.getByRole("button", { name: "Aplicar período", exact: true }).click();
  await expect(page.locator("main").getByRole("alert")).toHaveText("A data inicial deve ser anterior ou igual à data final.");
  await period(page, "01/01/2000", "01/01/2026");
  await expect(page.locator("main").getByRole("alert")).toHaveText("Selecione um período de no máximo 10 anos.");
  await period(page, "01/01/2025", "31/12/2026");
  await page.getByLabel("Agrupamento", { exact: true }).selectOption("DAY");
  await expect(chartPanel(page).getByRole("alert")).toContainText("366 dias");
  await page.getByLabel("Agrupamento", { exact: true }).selectOption("MONTH");
  await expect(chartPanel(page).getByRole("list")).toBeVisible();
  await expect(page.locator("#dashboard-start")).toHaveValue("01/01/2025");
});

test("401 no gráfico encaminha ao login", async ({ page }) => {
  await mockSummary(page);
  await page.route("**/dashboard/transactions-summary?*", route => route.fulfill({ status: 401, json: { message: "Sessão expirada" } }));
  await page.goto("/dashboard");
  await expect(page).toHaveURL(/\/login$/);
});

test("API real: receitas, despesas, dias zerados, meses e categorias", async ({ page, request }) => {
  const email = `charts-${Date.now()}@example.test`;
  const password = "Disposable-Chart-2026!";
  const registration = await request.post(`${apiURL}/users`, { data: { name: "Validação dos gráficos", email, password } });
  expect(registration.status()).toBe(201);
  const login = await request.post(`${apiURL}/auth/login`, { data: { email, password } });
  expect(login.ok()).toBe(true);
  const { accessToken } = await login.json();
  const headers = { Authorization: `Bearer ${accessToken}` };
  const account = await request.post(`${apiURL}/accounts`, { headers, data: { name: "Conta gráficos" } });
  const category = await request.post(`${apiURL}/categories`, { headers, data: { name: "Categoria gráficos" } });
  expect(account.ok()).toBe(true); expect(category.ok()).toBe(true);
  const accountId = (await account.json()).id;
  const categoryId = (await category.json()).id;
  for (const [amount, type, date] of [["0.10", "EXPENSE", "2026-10-01"], ["0.20", "EXPENSE", "2026-10-03"], ["100.00", "INCOME", "2026-10-01"]]) {
    const response = await request.post(`${apiURL}/transactions`, { headers, data: { description: "Dados de validação", amount, type, date, accountId, categoryId } });
    expect(response.ok()).toBe(true);
  }
  await page.addInitScript(token => sessionStorage.setItem("accessToken", token), accessToken);
  await page.goto("/dashboard");
  await period(page);
  const panel = chartPanel(page);
  await expect(panel.getByRole("list")).toContainText("0,30");
  await page.getByLabel("Agrupamento", { exact: true }).selectOption("DAY");
  await expect(panel.getByRole("list").getByRole("listitem")).toHaveCount(3);
  await expect(panel.getByRole("list").getByRole("listitem").nth(1)).toContainText("0,00");
  await page.getByLabel("Tipo de gráfico", { exact: true }).selectOption("BAR");
  await page.getByLabel("Movimentação", { exact: true }).selectOption("INCOME");
  await expect(panel.getByRole("list").getByRole("listitem").first()).toContainText("100,00");
  await page.getByLabel("Agrupamento", { exact: true }).selectOption("MONTH");
  await expect(panel.getByRole("list")).toContainText("outubro/2026");
  await expect(panel.getByRole("list")).toContainText("100,00");
  await page.getByLabel("Agrupamento", { exact: true }).selectOption("CATEGORY");
  await expect(panel.getByRole("list")).toContainText("Categoria gráficos");
  await page.screenshot({ path: "test-results/charts-real-api.png", fullPage: true });
});
