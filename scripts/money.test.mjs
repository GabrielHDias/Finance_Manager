import { test, after } from 'node:test';
import assert from 'node:assert/strict';
import { mkdtempSync, readFileSync, writeFileSync, rmSync } from 'node:fs';
import { tmpdir } from 'node:os';
import { join } from 'node:path';
import { createRequire } from 'node:module';
import ts from '../frontend/node_modules/typescript/lib/typescript.js';
const temp = mkdtempSync(join(tmpdir(), 'fm-money-'));
after(() => rmSync(temp, {recursive:true,force:true}));
for (const name of ['money','dates','formatters','transaction-csv']) {
  const source = readFileSync(new URL(`../frontend/src/lib/${name}.ts`, import.meta.url),'utf8');
  writeFileSync(join(temp,`${name}.js`),ts.transpileModule(source,{compilerOptions:{module:ts.ModuleKind.CommonJS,target:ts.ScriptTarget.ES2021}}).outputText);
}
const require = createRequire(import.meta.url);
const {cents,decimal,validInput} = require(join(temp,'money.js'));
const {formatCurrency} = require(join(temp,'formatters.js'));
const {serializeTransactionCsv} = require(join(temp,'transaction-csv.js'));
test('exact sums, negative differences, totals above input range and limit rejection',()=>{
 assert.equal(decimal(cents('0.10')+cents('0.20')),'0.30');
 assert.equal(decimal(cents('0.10')-cents('0.20')),'-0.10');
 assert.equal(decimal(cents('99999999999999999.99')*BigInt(2)),'199999999999999999.98');
 for(const value of ['0','-1','100000000000000000.00','1.001','NaN','1e3']) assert.equal(validInput(value),false);
 assert.equal(validInput('99999999999999999.99'),true);
});
test('JSON, currency and CSV preserve the maximum exactly',()=>{
 const amount=JSON.parse(JSON.stringify({amount:'99999999999999999.99'})).amount;
 assert.equal(formatCurrency(amount),'R$\u00a099.999.999.999.999.999,99');
 const csv=serializeTransactionCsv([{date:'2026-10-20',amount,description:'Teste',type:'INCOME',accountName:'A',categoryName:'C'}]);
 assert.ok(csv.includes('"99999999999999999,99"'));
 assert.equal(csv.match(/"(\d+,\d{2})"/)[1].replace(',','.'),amount);
});
