import { randomBytes } from 'node:crypto';
import { readFile, writeFile } from 'node:fs/promises';
import { dirname, join } from 'node:path';
import { fileURLToPath } from 'node:url';

const root = join(dirname(fileURLToPath(import.meta.url)), '..');
const envPath = join(root, '.env');
const template = await readFile(join(root, '.env.example'), 'utf8');
let original;
try {
  original = await readFile(envPath, 'utf8');
} catch (error) {
  if (error.code !== 'ENOENT') throw error;
}
let content = original ?? template;
for (const [key, createValue] of [
  ['DB_PASSWORD', () => randomBytes(32).toString('hex')],
  ['JWT_SECRET', () => randomBytes(32).toString('base64')],
]) {
  const pattern = new RegExp(`^(?:export\\s+)?${key}\\s*=([^\\r\\n]*)`, 'm');
  const match = content.match(pattern);
  const value = match?.[1].trim();
  if (!match) {
    content += `\n${key}=${createValue()}\n`;
  } else if (!value || value === '""' || value === "''" || value.startsWith('#')) {
    content = content.replace(pattern, `${key}=${createValue()}`);
  }
}
if (content !== original) {
  await writeFile(envPath, content, { mode: 0o600, ...(original === undefined ? { flag: 'wx' } : {}) });
  console.log(original === undefined ? 'Criado .env com credenciais locais.' : 'Preenchidas variáveis ausentes ou vazias em .env. Valores existentes preservados.');
} else {
  console.log('.env existente preservado.');
}
const frontendEnv = join(root, 'frontend', '.env.local');
try {
  await writeFile(frontendEnv, await readFile(join(root, 'frontend', '.env.example')), { flag: 'wx', mode: 0o600 });
  console.log('Criado frontend/.env.local.');
} catch (error) {
  if (error.code !== 'EEXIST') throw error;
  console.log('frontend/.env.local existente preservado.');
}
