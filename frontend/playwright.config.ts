import { defineConfig } from "@playwright/test";
const baseURL = process.env.E2E_BASE_URL ?? "http://127.0.0.1:3000";
if (!["localhost", "127.0.0.1", "[::1]"].includes(new URL(baseURL).hostname)) {
  throw new Error("E2E requires an explicit local disposable application");
}
export default defineConfig({
  testDir: "./e2e", workers: 1, retries: 0, timeout: 45000,
  reporter: [["list"], ["json", { outputFile: "test-results/results.json" }]],
  use: { baseURL, browserName: "chromium", actionTimeout: 10000, trace: "off", screenshot: "off", video: "off" },
});
