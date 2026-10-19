import assert from 'node:assert/strict';
import { randomBytes, randomUUID } from 'node:crypto';

const api = process.env.SMOKE_API_URL?.replace(/\/$/, '');
const frontend = process.env.SMOKE_FRONTEND_URL?.replace(/\/$/, '');
assert.ok(api && frontend, 'Defina SMOKE_API_URL e SMOKE_FRONTEND_URL para o ambiente de teste.');
let token;
let count = 0;
async function request(method, path, body, expected = 200, authenticated = true) {
  const response = await fetch(`${api}${path}`, {
    method,
    headers: { 'Content-Type': 'application/json', ...(authenticated && token ? { Authorization: `Bearer ${token}` } : {}) },
    ...(body === undefined ? {} : { body: JSON.stringify(body) }),
    signal: AbortSignal.timeout(15000),
  });
  const content = await response.text();
  assert.equal(response.status, expected, `${method} ${path}: ${response.status} ${content}`);
  count++;
  return content ? JSON.parse(content) : undefined;
}

const page = await fetch(`${frontend}/login`, { signal: AbortSignal.timeout(15000) });
assert.equal(page.status, 200, 'A página de login deve responder.');
const html = await page.text();
assert.ok(html.includes('<form'), 'A página de login deve conter o formulário.');
const assets = [...html.matchAll(/src="([^\"]+\.js[^\"]*)"/g)].map(match => match[1]);
assert.ok(assets.length > 0, 'O frontend deve servir seus arquivos JavaScript.');
for (const asset of assets) {
  const response = await fetch(new URL(asset, frontend), { signal: AbortSignal.timeout(15000) });
  assert.equal(response.status, 200, `Arquivo do frontend indisponível: ${asset}`);
}

const email = `smoke-${randomUUID()}@example.test`;
const password = randomBytes(24).toString('base64url');
await request('POST', '/users', { name: 'Validação automatizada', email, password }, 201, false);
token = (await request('POST', '/auth/login', { email, password }, 200, false)).accessToken;
assert.ok(token);
const account = await request('POST', '/accounts', { name: 'Conta de validação' }, 201);
const category = await request('POST', '/categories', { name: 'Categoria de validação' }, 201);
const common = { accountId: account.id, categoryId: category.id };
const income = await request('POST', '/transactions', { ...common, description: 'Receita de validação', amount: 1000, type: 'INCOME', date: '2026-10-01' }, 201);
const expense = await request('POST', '/transactions', { ...common, description: 'Despesa de validação', amount: 100, type: 'EXPENSE', date: '2026-10-15' }, 201);
assert.equal((await request('GET', `/accounts/${account.id}/balance`)).balance, 900);
assert.deepEqual(await request('GET', '/dashboard/summary?startDate=2026-10-01&endDate=2026-10-31'), { totalIncome: 1000, totalExpense: 100, balance: 900 });
await request('PUT', `/accounts/${account.id}`, { name: 'Conta renomeada' });
await request('PUT', `/categories/${category.id}`, { name: 'Categoria renomeada' });
let expenses = await request('GET', '/dashboard/expenses-by-category?startDate=2026-10-01&endDate=2026-10-31');
assert.equal(expenses[0].categoryName, 'Categoria renomeada');
assert.equal(expenses[0].amount, 100);
await request('PUT', `/transactions/${expense.id}`, { ...common, description: 'Despesa editada', amount: 150, type: 'EXPENSE', date: '2026-11-01' });
const filtered = await request('GET', `/transactions?startDate=2026-11-01&endDate=2026-11-30&type=EXPENSE&accountId=${account.id}&categoryId=${category.id}`);
assert.equal(filtered.length, 1);
assert.equal(filtered[0].amount, 150);
assert.equal(filtered[0].accountName, 'Conta renomeada');
assert.equal(filtered[0].categoryName, 'Categoria renomeada');
assert.deepEqual(await request('GET', '/dashboard/summary?startDate=2026-10-01&endDate=2026-10-31'), { totalIncome: 1000, totalExpense: 0, balance: 1000 });
assert.deepEqual(await request('GET', '/dashboard/expenses-by-category?startDate=2026-10-01&endDate=2026-10-31'), []);
assert.equal((await request('GET', `/accounts/${account.id}/balance`)).balance, 850);
await request('DELETE', `/accounts/${account.id}`, undefined, 409);
await request('DELETE', `/categories/${category.id}`, undefined, 409);
await request('POST', '/transactions', { ...common, description: 'Valor inválido', amount: 0, type: 'EXPENSE', date: '2026-10-01' }, 400);
await request('GET', '/dashboard/summary', undefined, 401, false);
await request('DELETE', `/transactions/${expense.id}`, undefined, 204);
assert.equal((await request('GET', `/accounts/${account.id}/balance`)).balance, 1000);
await request('DELETE', `/transactions/${income.id}`, undefined, 204);
await request('DELETE', `/categories/${category.id}`, undefined, 204);
await request('DELETE', `/accounts/${account.id}`, undefined, 204);
await request('GET', `/accounts/${account.id}`, undefined, 404);
console.log(`Validação integrada concluída: frontend, arquivos estáticos e ${count} verificações HTTP da API.`);
