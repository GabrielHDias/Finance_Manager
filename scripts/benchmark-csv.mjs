// Run with node --expose-gc scripts/benchmark-csv.mjs; synthetic data only.
import { mkdtempSync, readFileSync, writeFileSync, rmSync } from 'node:fs';
import { tmpdir } from 'node:os';
import { join } from 'node:path';
import { createRequire } from 'node:module';
import ts from '../frontend/node_modules/typescript/lib/typescript.js';
const temp=mkdtempSync(join(tmpdir(),'fm-csv-benchmark-'));
try {
 for(const name of ['money','dates','transaction-csv']) {
  const source=readFileSync(new URL(`../frontend/src/lib/${name}.ts`,import.meta.url),'utf8');
  writeFileSync(join(temp,`${name}.js`),ts.transpileModule(source,{compilerOptions:{module:ts.ModuleKind.CommonJS,target:ts.ScriptTarget.ES2021}}).outputText);
 }
 const {serializeTransactionCsv}=createRequire(import.meta.url)(join(temp,'transaction-csv.js'));
 global.gc?.();const initial=process.memoryUsage().heapUsed;const started=performance.now();
 const rows=Array.from({length:10000},(_,id)=>({id,date:'2026-10-20',amount:'99999999999999999.99',type:'EXPENSE',
  description:'🔒'.repeat(127)+'x',accountName:'🔒'.repeat(50),categoryName:'🔒'.repeat(50)}));
 const csv=serializeTransactionCsv(rows);
 const result={rows:rows.length,bytes:Buffer.byteLength(csv),heapDeltaBytes:process.memoryUsage().heapUsed-initial,
  durationMs:performance.now()-started,node:process.version};
 if(result.heapDeltaBytes>64*1024*1024)throw Error('CSV exceeded the 64 MiB heap budget');
 console.log(JSON.stringify(result,null,2));
} finally {rmSync(temp,{recursive:true,force:true});}
