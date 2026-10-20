import { test } from 'node:test';
import assert from 'node:assert/strict';
import { readFileSync } from 'node:fs';
import vm from 'node:vm';
import ts from '../frontend/node_modules/typescript/lib/typescript.js';

const source = readFileSync(new URL('../frontend/src/lib/theme.ts', import.meta.url), 'utf8');
function environment({ saved, dark = false, blocked = false } = {}) {
  const events = new EventTarget();
  const media = new EventTarget();
  media.matches = dark;
  const document = { documentElement: { dataset: {} } };
  const localStorage = {
    getItem: () => { if (blocked) throw Error('blocked'); return saved ?? null; },
    setItem: (_, value) => { if (blocked) throw Error('blocked'); saved = value; },
  };
  const window = { localStorage, matchMedia: () => media,
    addEventListener: events.addEventListener.bind(events), removeEventListener: events.removeEventListener.bind(events), dispatchEvent: events.dispatchEvent.bind(events) };
  const context = vm.createContext({ exports: {}, document, window, localStorage, matchMedia: window.matchMedia, Event });
  vm.runInContext(ts.transpileModule(source, { compilerOptions: { module: ts.ModuleKind.CommonJS } }).outputText, context);
  vm.runInContext(context.exports.themeInitializer, context);
  return { api: context.exports, document, media, window, saved: () => saved };
}

test('initialization prefers valid saved theme, otherwise follows system, including blocked storage', () => {
  for (const [settings, expected] of [[{saved:'light',dark:true},'light'],[{saved:'dark'},'dark'],[{dark:true},'dark'],[{saved:'invalid',dark:true},'dark'],[{blocked:true,dark:true},'dark'],[{},'light']]) {
    const env=environment(settings); assert.equal(env.api.themeSnapshot(),expected);
  }
});

test('system changes apply until a manual selection and persist across a new browser context', () => {
  const env=environment(); let changes=0;
  const cleanup=env.api.subscribeTheme(()=>changes++);
  env.media.matches=true; env.media.dispatchEvent(new Event('change'));
  assert.equal(env.api.themeSnapshot(),'dark');
  env.api.setTheme('light');
  env.media.dispatchEvent(new Event('change'));
  assert.equal(env.api.themeSnapshot(),'light'); assert.equal(env.saved(),'light');
  assert.equal(environment({saved:env.saved(),dark:true}).api.themeSnapshot(),'light');
  cleanup(); const previous=changes;
  env.media.dispatchEvent(new Event('change')); assert.equal(changes,previous);
});

test('manual selection survives system changes when storage is unavailable', () => {
  const env=environment({blocked:true}); env.api.subscribeTheme(()=>{});
  env.api.setTheme('dark'); env.media.dispatchEvent(new Event('change'));
  assert.equal(env.api.themeSnapshot(),'dark');
});

test('another tab removing a preference restores system theme', () => {
  const env=environment({dark:true}); env.api.subscribeTheme(()=>{});
  env.api.setTheme('light'); env.window.localStorage.setItem('finance-manager-theme', 'invalid');
  const event=new Event('storage'); Object.defineProperty(event,'key',{value:'finance-manager-theme'});
  env.window.dispatchEvent(event); assert.equal(env.api.themeSnapshot(),'dark');
});
