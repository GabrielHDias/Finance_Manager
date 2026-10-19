import assert from 'node:assert/strict';
import { execFileSync } from 'node:child_process';
import { mkdtemp, mkdir, copyFile, readFile, writeFile, rm } from 'node:fs/promises';
import { tmpdir } from 'node:os';
import { dirname, join } from 'node:path';
import { fileURLToPath } from 'node:url';
import test from 'node:test';

const project = join(dirname(fileURLToPath(import.meta.url)), '..');
async function fixture(t) {
  const root = await mkdtemp(join(tmpdir(), 'finance-env-'));
  t.after(() => rm(root, { recursive: true, force: true }));
  await mkdir(join(root, 'scripts'));
  await mkdir(join(root, 'frontend'));
  await copyFile(join(project, 'scripts/init-env.mjs'), join(root, 'scripts/init-env.mjs'));
  await copyFile(join(project, '.env.example'), join(root, '.env.example'));
  await copyFile(join(project, 'frontend/.env.example'), join(root, 'frontend/.env.example'));
  return { root, run: () => execFileSync(process.execPath, ['scripts/init-env.mjs'], { cwd: root, encoding: 'utf8' }) };
}
function values(content) {
  return Object.fromEntries(content.split('\n').filter(line => line && !line.startsWith('#') && line.includes('=')).map(line => [line.slice(0, line.indexOf('=')), line.slice(line.indexOf('=') + 1)]));
}

test('creates valid random credentials and frontend configuration without logging secrets', async t => {
  const { root, run } = await fixture(t);
  const log = run();
  const env = values(await readFile(join(root, '.env'), 'utf8'));
  assert.match(env.DB_PASSWORD, /^[a-f0-9]{64}$/);
  assert.equal(Buffer.from(env.JWT_SECRET, 'base64').length, 32);
  assert.ok(!log.includes(env.DB_PASSWORD) && !log.includes(env.JWT_SECRET));
  assert.equal(await readFile(join(root, 'frontend/.env.local'), 'utf8'), await readFile(join(root, 'frontend/.env.example'), 'utf8'));
});

test('running twice does not rotate credentials or overwrite frontend settings', async t => {
  const { root, run } = await fixture(t);
  run();
  const before = await readFile(join(root, '.env'), 'utf8');
  await writeFile(join(root, 'frontend/.env.local'), 'NEXT_PUBLIC_API_URL=http://localhost:9090\n');
  run();
  assert.equal(await readFile(join(root, '.env'), 'utf8'), before);
  assert.equal(await readFile(join(root, 'frontend/.env.local'), 'utf8'), 'NEXT_PUBLIC_API_URL=http://localhost:9090\n');
});

test('adds a missing JWT secret while preserving an existing database password', async t => {
  const { root, run } = await fixture(t);
  await writeFile(join(root, '.env'), 'DB_PASSWORD=existing-test-password\nCUSTOM_VALUE=preserved\n');
  run();
  const env = values(await readFile(join(root, '.env'), 'utf8'));
  assert.equal(env.DB_PASSWORD, 'existing-test-password');
  assert.equal(env.CUSTOM_VALUE, 'preserved');
  assert.equal(Buffer.from(env.JWT_SECRET, 'base64').length, 32);
});

test('fills empty credentials without replacing a configured JWT secret', async t => {
  const { root, run } = await fixture(t);
  await writeFile(join(root, '.env'), 'DB_PASSWORD=\nJWT_SECRET="existing-test-secret"\n');
  run();
  const env = values(await readFile(join(root, '.env'), 'utf8'));
  assert.match(env.DB_PASSWORD, /^[a-f0-9]{64}$/);
  assert.equal(env.JWT_SECRET, '"existing-test-secret"');
});
