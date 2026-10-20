import { test, expect, type Page } from "@playwright/test";
import AxeBuilder from "@axe-core/playwright";

async function contrast(page: Page) {
  const result = await new AxeBuilder({ page }).withRules(["color-contrast"]).analyze();
  expect(result.violations.map(item => ({ id: item.id, count: item.nodes.length }))).toEqual([]);
}
for (const theme of ["light", "dark"] as const) {
  for (const width of [390, 1280]) {
    test(`${theme} ${width}: cadastro, login, CRUD, CSV, teclado e contraste`, async ({ browser }) => {
      const context = await browser.newContext({ colorScheme: theme, viewport: { width, height: 900 } });
      const page = await context.newPage();
      const errors: string[] = [];
      page.on("pageerror", error => errors.push(error.message));
      page.on("console", message => { if (message.type() === "error") errors.push(message.text()); });
      await page.addInitScript(() => {
        const observer = new MutationObserver(() => {
          const theme = document.documentElement?.dataset.theme;
          if (theme && !(window as Window & { firstTheme?: string }).firstTheme) {
            (window as Window & { firstTheme?: string }).firstTheme = theme;
          }
        });
        observer.observe(document, { subtree: true, attributes: true, childList: true });
      });
      await page.goto("/register");
      await expect(page.locator("html")).toHaveAttribute("data-theme", theme);
      expect(await page.evaluate(() => (window as Window & { firstTheme?: string }).firstTheme)).toBe(theme);
      await contrast(page);
      await page.emulateMedia({ colorScheme: theme === "dark" ? "light" : "dark" });
      await expect(page.locator("html")).toHaveAttribute("data-theme", theme === "dark" ? "light" : "dark");
      await page.emulateMedia({ colorScheme: theme });
      await expect(page.locator("html")).toHaveAttribute("data-theme", theme);
      const email = `e2e-${theme}-${width}-${Date.now()}@example.test`;
      await page.getByLabel("Nome", { exact: true }).fill("Pessoa de teste");
      await page.getByLabel("E-mail", { exact: true }).fill(email);
      await page.getByLabel("Senha", { exact: true }).fill("Disposable-Test-2026!");
      await page.locator('button[type="submit"]').click();
      await expect(page).toHaveURL(/\/login$/);
      await page.getByLabel("E-mail", { exact: true }).fill(email);
      await page.getByLabel("Senha", { exact: true }).fill("Disposable-Test-2026!");
      await page.getByLabel("Senha", { exact: true }).press("Enter");
      await expect(page).toHaveURL(/\/dashboard$/);
      await expect(page.getByRole("heading", { name: "Dashboard", exact: true })).toBeVisible();
      await contrast(page);
      await page.getByRole("link", { name: "Contas financeiras", exact: true }).click();
      await expect(page.getByText("Nenhuma conta cadastrada.")).toBeVisible();
      await page.getByLabel("Nome", { exact: true }).fill("Conta E2E");
      await page.getByRole("button", { name: "Criar conta", exact: true }).click();
      await expect(page.getByText("Conta criada.", { exact: true })).toBeVisible();
      await page.getByRole("link", { name: "Categorias", exact: true }).click();
      await expect(page.getByRole("heading", { name: "Categorias", exact: true })).toBeVisible();
      await page.getByLabel("Nome", { exact: true }).fill("Categoria E2E");
      await page.getByRole("button", { name: "Criar categoria", exact: true }).click();
      await expect(page.getByText("Categoria criada.", { exact: true })).toBeVisible();
      await page.getByRole("link", { name: "Transações", exact: true }).click();
      await expect(page.getByText("Nenhuma transação encontrada.")).toBeVisible();
      await page.locator("#description").fill("Transação E2E");
      await page.locator("#amount").fill("99999999999999999.99");
      await page.locator("#date").fill("20/10/2026");
      await page.locator("#account").selectOption({ label: "Conta E2E" });
      await page.locator("#category").selectOption({ label: "Categoria E2E" });
      await page.locator('form.transaction-form button[type="submit"]').first().click();
      await expect(page.getByText("Transação criada.", { exact: true })).toBeVisible();
      await expect(page.locator(".transaction-amount")).toContainText("99.999.999.999.999.999,99");
      await page.getByRole("button", { name: "Editar Transação E2E", exact: true }).click();
      await expect(page.locator("#amount")).toHaveValue("99999999999999999.99");
      await page.locator("#amount").fill("0.30");
      await page.locator('form.transaction-form button[type="submit"]').first().click();
      await expect(page.getByText("Transação atualizada.", { exact: true })).toBeVisible();
      const download = page.waitForEvent("download");
      await page.getByRole("button", { name: "Exportar CSV", exact: true }).click();
      const stream = await (await download).createReadStream();
      const chunks = []; for await (const chunk of stream!) chunks.push(chunk);
      expect(Buffer.concat(chunks).toString("utf8")).toContain('"0,30"');
      await contrast(page);
      await page.getByRole("link", { name: "Dashboard", exact: true }).click();
      await page.locator("#dashboard-start").fill("01/10/2026");
      await page.locator("#dashboard-end").fill("31/10/2026");
      await page.getByRole("button", { name: "Aplicar período", exact: true }).click();
      await expect(page.getByText("Total de despesas", { exact: false })).toContainText("0,30");
      await expect(page.getByRole("list", { name: "Despesas por categoria: valores e participação" })).toContainText("0,30");
      await contrast(page);
      await page.getByRole("link", { name: "Orçamentos", exact: true }).click();
      await expect(page.locator("#budget-limit")).toBeVisible();
      await page.locator("#budget-month").selectOption("10");
      await page.locator("#budget-year").fill("2026");
      await page.getByRole("button", { name: "Consultar mês", exact: true }).click();
      await expect(page.getByRole("heading", { name: "Definir limite — Outubro de 2026" })).toBeVisible();
      await page.locator("#budget-category").selectOption({ label: "Categoria E2E" });
      await page.locator("#budget-limit").fill("0.20");
      await page.getByRole("button", { name: "Salvar limite", exact: true }).click();
      await expect(page.locator(".budget-card")).toContainText("Limite excedido");
      await expect(page.locator(".budget-card dd").last()).toContainText("0,10");
      await contrast(page);
      await page.getByRole("link", { name: "Transações", exact: true }).click();
      await page.getByRole("button", { name: "Excluir Transação E2E", exact: true }).click();
      await page.getByRole("button", { name: "Confirmar exclusão", exact: true }).click();
      await expect(page.getByText("Nenhuma transação encontrada.")).toBeVisible();
      const toggle = page.getByRole("button", { name: "Tema escuro", exact: true });
      await toggle.focus(); await page.keyboard.press("Space");
      const selected = theme === "dark" ? "light" : "dark";
      await expect(page.locator("html")).toHaveAttribute("data-theme", selected);
      await page.reload(); await expect(page.locator("html")).toHaveAttribute("data-theme", selected);
      expect(await page.evaluate(() => (window as Window & { firstTheme?: string }).firstTheme)).toBe(selected);
      await expect(page.getByText("Nenhuma transação encontrada.")).toBeVisible();
      expect(await page.evaluate(() => document.documentElement.scrollWidth <= innerWidth)).toBe(true);
      // Exercise an actual loading render, followed by a controlled transport error.
      let release!: () => void;
      const barrier = new Promise<void>(resolve => { release = resolve; });
      await page.route("**/transactions/page?**", async route => { await barrier; await route.fulfill({ status: 503,
        contentType: "application/json", body: JSON.stringify({ message: "Serviço indisponível para teste" }) }); });
      await page.reload({ waitUntil: "domcontentloaded" });
      await expect(page.getByText("Carregando transações...", { exact: true })).toBeVisible();
      release();
      await expect(page.locator("p.error-message[role=alert]")).toContainText("Serviço indisponível para teste");
      await contrast(page);
      expect(errors.filter(error => !error.includes("503 (Service Unavailable)"))).toEqual([]);
      await context.close();
    });
  }
}

test("landing ignora token expirado e 401 limpa a sessão", async ({ page }) => {
  const expired = `e30.${Buffer.from(JSON.stringify({ exp: 1 })).toString("base64url")}.invalid`;
  await page.addInitScript(value => sessionStorage.setItem("accessToken", value), expired);
  await page.goto("/");
  await expect(page.getByRole("link", { name: "Entrar", exact: true }).last()).toHaveAttribute("href", "/login");
  await page.goto("/dashboard");
  await expect(page).toHaveURL(/\/login$/);
  expect(await page.evaluate(() => sessionStorage.getItem("accessToken"))).toBeNull();
});
