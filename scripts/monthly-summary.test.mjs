import assert from 'node:assert/strict';
import { mkdtempSync, readFileSync, writeFileSync, rmSync } from 'node:fs';
import { tmpdir } from 'node:os';
import { join } from 'node:path';
import { createRequire } from 'node:module';
import { after, test } from 'node:test';
import ts from '../frontend/node_modules/typescript/lib/typescript.js';

const temp = mkdtempSync(join(tmpdir(), 'finance-monthly-test-'));
after(() => rmSync(temp, { recursive: true, force: true }));
for (const name of ['money', 'dates', 'monthly-summary']) {
  const source = readFileSync(new URL(`../frontend/src/lib/${name}.ts`, import.meta.url), 'utf8');
  const { outputText } = ts.transpileModule(source, { compilerOptions: { module: ts.ModuleKind.CommonJS, target: ts.ScriptTarget.ES2021 } });
  writeFileSync(join(temp, `${name}.js`), outputText);
}
const { monthPeriod, monthlyPeriods, monthLabel, monthlyDifference } = createRequire(import.meta.url)(join(temp, 'monthly-summary.js'));

test('January compares with December of the previous year', () => {
  assert.deepEqual(monthlyPeriods('2026-01'), {
    reference: '2026-01', previousReference: '2025-12',
    current: { startDate: '2026-01-01', endDate: '2026-01-31' },
    previous: { startDate: '2025-12-01', endDate: '2025-12-31' },
  });
});

test('calendar bounds include leap days and handle century rules', () => {
  assert.equal(monthPeriod('2024-02').endDate, '2024-02-29');
  assert.equal(monthPeriod('2026-02').endDate, '2026-02-28');
  assert.equal(monthPeriod('1900-02').endDate, '1900-02-28');
  assert.equal(monthPeriod('2000-02').endDate, '2000-02-29');
  assert.equal(monthPeriod('2026-04').endDate, '2026-04-30');
  assert.equal(monthlyPeriods('2024-03').previous.endDate, '2024-02-29');
});

test('rejects invalid references and a comparison outside the supported calendar', () => {
  for (const value of ['', '2026-00', '2026-13', '26-01', '0000-01', '2026-1', '2026-01-01']) assert.throws(() => monthlyPeriods(value));
  assert.throws(() => monthlyPeriods('0001-01'));
  assert.equal(monthlyPeriods('0001-02').previous.startDate, '0001-01-01');
  assert.equal(monthPeriod('9999-12').endDate, '9999-12-31');
});

test('labels months in Portuguese independently of locale or timezone', () => {
  assert.equal(monthLabel('2026-03'), 'Março de 2026');
  assert.equal(monthLabel('2025-12'), 'Dezembro de 2025');
});

test('differences work with cents, zero bases and negative balances', () => {
  assert.equal(monthlyDifference("0.3", "0.1"), "0.20");
  assert.equal(monthlyDifference("100", "0"), "100.00");
  assert.equal(monthlyDifference("0", "100"), "-100.00");
  assert.equal(monthlyDifference("-50", "-100"), "50.00");
  assert.equal(monthlyDifference("-100", "50"), "-150.00");
  assert.equal(monthlyDifference("0", "0"), "0.00");
});
