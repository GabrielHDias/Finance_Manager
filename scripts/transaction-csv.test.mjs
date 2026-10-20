import assert from 'node:assert/strict';
import { mkdtempSync, readFileSync, writeFileSync, rmSync } from 'node:fs';
import { tmpdir } from 'node:os';
import { join } from 'node:path';
import { createRequire } from 'node:module';
import { after, test } from 'node:test';
import ts from '../frontend/node_modules/typescript/lib/typescript.js';

// Run the actual TypeScript serializer with the existing compiler, without a test dependency.
const temp = mkdtempSync(join(tmpdir(), 'finance-csv-test-'));
after(() => rmSync(temp, { recursive: true, force: true }));
for (const name of ['money', 'dates', 'transaction-csv']) {
  const source = readFileSync(new URL(`../frontend/src/lib/${name}.ts`, import.meta.url), 'utf8');
  const { outputText } = ts.transpileModule(source, {
    compilerOptions: { module: ts.ModuleKind.CommonJS, target: ts.ScriptTarget.ES2021 },
  });
  writeFileSync(join(temp, `${name}.js`), outputText);
}
const { serializeTransactionCsv } = createRequire(import.meta.url)(join(temp, 'transaction-csv.js'));
const row = {
  id: 1, date: '2026-10-19', description: 'Salário', amount: "1234.50", type: 'INCOME',
  accountId: 1, accountName: 'Conta principal', categoryId: 1, categoryName: 'Trabalho',
};

test('exports Brazilian dates, decimals, types, names and UTF-8 BOM with CRLF', () => {
  assert.equal(serializeTransactionCsv([row]), '\uFEFFdata;descricao;valor;tipo;conta;categoria\r\n"19/10/2026";"Salário";"1234,50";"RECEITA";"Conta principal";"Trabalho"\r\n');
  assert.match(serializeTransactionCsv([{ ...row, amount: "0.01", type: 'EXPENSE' }]), /"0,01";"DESPESA"/);
});

test('quotes separators, embedded quotes and multiline descriptions without losing data', () => {
  const csv = serializeTransactionCsv([{ ...row, description: 'Mercado; "feira"\nAçúcar', accountName: 'Banco; A', categoryName: 'Café "especial"' }]);
  assert.ok(csv.includes('"Mercado; ""feira""\nAçúcar"'));
  assert.ok(csv.includes('"Banco; A";"Café ""especial"""'));
});

test('neutralizes spreadsheet formula triggers in every user-controlled text column', () => {
  for (const value of ['=1+1', '+SUM(A1)', '-1+2', '@SUM(A1)', '\t=1', '\r=1', '\n=1', '  =1']) {
    const csv = serializeTransactionCsv([{ ...row, description: value, accountName: value, categoryName: value }]);
    assert.equal(csv.split(`"'${value}"`).length - 1, 3, JSON.stringify(value));
  }
});

test('preserves ordinary text, row order and input data', () => {
  const rows = [row, { ...row, id: 2, description: "Compra d'água", date: '2024-02-29' }];
  const before = structuredClone(rows);
  const csv = serializeTransactionCsv(rows);
  assert.ok(csv.indexOf('Salário') < csv.indexOf("Compra d'água"));
  assert.ok(csv.includes('"29/02/2024"'));
  assert.deepEqual(rows, before);
});

test('empty result has only the header and no phantom transaction', () => {
  assert.equal(serializeTransactionCsv([]), '\uFEFFdata;descricao;valor;tipo;conta;categoria\r\n');
});
