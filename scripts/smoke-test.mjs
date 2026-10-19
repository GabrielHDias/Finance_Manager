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

async function csvRequest(action, csv, accountId, categoryId, previewHash, expected = 200, authenticated = true) {
  const body = new FormData();
  body.set('file', new Blob([csv], { type: 'text/csv' }), 'transactions.csv');
  body.set('accountId', String(accountId));
  body.set('categoryId', String(categoryId));
  if (previewHash) body.set('previewHash', previewHash);
  const response = await fetch(`${api}/transactions/import/${action}`, {
    method: 'POST', body, headers: authenticated && token ? { Authorization: `Bearer ${token}` } : {},
    signal: AbortSignal.timeout(20000),
  });
  const result = await response.json();
  assert.equal(response.status, expected, `CSV ${action}: ${response.status} ${JSON.stringify(result)}`);
  count++;
  return result;
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

// Exercise the real password encoder at its UTF-8 boundary, including Unicode.
for (const password of ['a'.repeat(73), 'é'.repeat(37), '🔒'.repeat(19), 'curta']) {
  const error = await request('POST', '/users', { name: 'Limite de senha', email: `smoke-${randomUUID()}@example.test`, password }, 400, false);
  assert.match(error.fields.password ?? error.message, /senha/i);
  assert.ok(!error.message.includes('password cannot'), 'O erro deve orientar o usuário em português.');
}
for (const password of ['a'.repeat(72), 'é'.repeat(36), '🔒'.repeat(18)]) {
  const email = `smoke-${randomUUID()}@example.test`;
  const user = await request('POST', '/users', { name: 'Limite aceito', email, password }, 201, false);
  assert.deepEqual(Object.keys(user).sort(), ['email', 'id', 'name']);
  assert.ok((await request('POST', '/auth/login', { email, password }, 200, false)).accessToken);
}

const email = `smoke-${randomUUID()}@example.test`;
const password = randomBytes(24).toString('base64url');
await request('POST', '/users', { name: 'Validação automatizada', email, password }, 201, false);
await request('POST', '/users', { name: 'Duplicado', email, password }, 409, false);
await request('POST', '/auth/login', { email, password: 'incorrect-password' }, 401, false);
token = (await request('POST', '/auth/login', { email: email.toUpperCase(), password }, 200, false)).accessToken;
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
// A second user must not see or mutate the first user's financial data.
const ownerToken = token;
const secondEmail = `smoke-${randomUUID()}@example.test`;
await request('POST', '/users', { name: 'Validação de isolamento', email: secondEmail, password }, 201, false);
token = (await request('POST', '/auth/login', { email: secondEmail, password }, 200, false)).accessToken;
for (const path of ['/accounts', '/categories', '/transactions', `/transactions?accountId=${account.id}&categoryId=${category.id}`]) {
  assert.deepEqual(await request('GET', path), [], `Dados de outro usuário expostos em ${path}`);
}
assert.deepEqual(await request('GET', '/dashboard/summary'), { totalIncome: 0, totalExpense: 0, balance: 0 });
assert.deepEqual(await request('GET', '/dashboard/expenses-by-category'), []);
for (const [path, body] of [
  [`/accounts/${account.id}`, { name: 'Alteração indevida' }],
  [`/categories/${category.id}`, { name: 'Alteração indevida' }],
  [`/transactions/${expense.id}`, { ...common, description: 'Alteração indevida', amount: 1, type: 'EXPENSE', date: '2026-11-01' }],
]) {
  await request('GET', path, undefined, 404);
  await request('PUT', path, body, 404);
  await request('DELETE', path, undefined, 404);
}
await request('GET', `/accounts/${account.id}/balance`, undefined, 404);
const secondAccount = await request('POST', '/accounts', { name: 'Conta do segundo usuário' }, 201);
const secondCategory = await request('POST', '/categories', { name: 'Categoria do segundo usuário' }, 201);
for (const links of [
  { accountId: account.id, categoryId: secondCategory.id },
  { accountId: secondAccount.id, categoryId: category.id },
]) {
  await request('POST', '/transactions', { ...links, description: 'Vínculo indevido', amount: 1, type: 'EXPENSE', date: '2026-10-19' }, 404);
}
const foreignCsv = 'data;descricao;valor;tipo\n2026-10-19;Teste de isolamento CSV;1;RECEITA';
await csvRequest('preview', foreignCsv, account.id, secondCategory.id, undefined, 404);
await csvRequest('preview', foreignCsv, secondAccount.id, category.id, undefined, 404);
await csvRequest('confirm', foreignCsv, account.id, category.id, 'invalid-hash', 404);
await request('DELETE', `/categories/${secondCategory.id}`, undefined, 204);
await request('DELETE', `/accounts/${secondAccount.id}`, undefined, 204);
token = ownerToken;
assert.equal((await request('GET', `/accounts/${account.id}`)).name, 'Conta renomeada');
assert.equal((await request('GET', `/categories/${category.id}`)).name, 'Categoria renomeada');
assert.equal((await request('GET', `/transactions/${expense.id}`)).amount, 150);
assert.equal((await request('GET', `/accounts/${account.id}/balance`)).balance, 850);
await request('POST', '/accounts', { name: 'Conta renomeada' }, 409);
await request('POST', '/categories', { name: 'Categoria renomeada' }, 409);
for (const path of ['/transactions', '/dashboard/summary', '/dashboard/expenses-by-category']) {
  await request('GET', `${path}?startDate=2026-11-01&endDate=2026-10-01`, undefined, 409);
}
for (const path of ['/accounts', '/categories', '/transactions', '/dashboard/summary', '/dashboard/expenses-by-category']) {
  await request('GET', path, undefined, 401, false);
}
token = 'invalid-token';
await request('GET', '/accounts', undefined, 401);
token = ownerToken;

// CSV preview must not write; invalid or changed inputs must not partially import.
const header = 'data;descricao;valor;tipo\n';
const csv = header + '2026-10-03;CSV receita;500;RECEITA\n04/10/2026;CSV despesa;25,00;DESPESA\n2026-10-04;CSV despesa;25.0;EXPENSE';
await csvRequest('preview', csv, account.id, category.id, undefined, 401, false);
const csvPreview = await csvRequest('preview', csv, account.id, category.id);
assert.equal(csvPreview.ready, 2);
assert.equal(csvPreview.duplicates, 1);
assert.equal(csvPreview.invalid, 0);
assert.equal((await request('GET', '/transactions')).length, 2);
const invalidCsv = csv + '\n2026-02-31;Inválida;0;DESPESA';
const invalidPreview = await csvRequest('preview', invalidCsv, account.id, category.id);
assert.equal(invalidPreview.invalid, 1);
await csvRequest('confirm', invalidCsv, account.id, category.id, invalidPreview.hash, 409);
await csvRequest('confirm', csv + '\n2026-10-19;Mudou;1;RECEITA', account.id, category.id, csvPreview.hash, 409);
assert.equal((await request('GET', '/transactions')).length, 2);
assert.deepEqual(await csvRequest('confirm', csv, account.id, category.id, csvPreview.hash), { imported: 2, skipped: 1 });
assert.deepEqual(await csvRequest('confirm', csv, account.id, category.id, csvPreview.hash), { imported: 0, skipped: 3 });
assert.equal((await request('GET', `/accounts/${account.id}/balance`)).balance, 1325);
const manualDuplicate = await csvRequest('preview', header + '2026-11-01;Despesa editada;150;DESPESA', account.id, category.id);
assert.equal(manualDuplicate.duplicates, 1);
assert.equal(manualDuplicate.ready, 0);
const concurrentCsv = header + '2026-10-19;CSV concorrente;10;RECEITA';
const concurrentPreview = await csvRequest('preview', concurrentCsv, account.id, category.id);
const simultaneous = await Promise.all([
  csvRequest('confirm', concurrentCsv, account.id, category.id, concurrentPreview.hash),
  csvRequest('confirm', concurrentCsv, account.id, category.id, concurrentPreview.hash),
]);
assert.equal(simultaneous.reduce((sum, item) => sum + item.imported, 0), 1);
assert.equal(simultaneous.reduce((sum, item) => sum + item.skipped, 0), 1);
await csvRequest('preview', header + '2026-10-19;"aspas sem fechar;1;RECEITA', account.id, category.id, undefined, 400);
await csvRequest('preview', new Uint8Array([0xc3, 0x28]), account.id, category.id, undefined, 400);
await csvRequest('preview', 'x'.repeat(1024 * 1024 + 100), account.id, category.id, undefined, 413);
const imported = (await request('GET', '/transactions')).filter(item => item.description.startsWith('CSV '));
assert.equal(imported.length, 3);
for (const item of imported) await request('DELETE', `/transactions/${item.id}`, undefined, 204);
assert.equal((await request('GET', `/accounts/${account.id}/balance`)).balance, 850);

await request('DELETE', `/transactions/${expense.id}`, undefined, 204);
assert.equal((await request('GET', `/accounts/${account.id}/balance`)).balance, 1000);
await request('DELETE', `/transactions/${income.id}`, undefined, 204);
await request('DELETE', `/categories/${category.id}`, undefined, 204);
await request('DELETE', `/accounts/${account.id}`, undefined, 204);
await request('GET', `/accounts/${account.id}`, undefined, 404);
console.log(`Validação integrada concluída: frontend, arquivos estáticos e ${count} verificações HTTP da API.`);
