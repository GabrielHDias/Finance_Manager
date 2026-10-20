import { test } from 'node:test';
import assert from 'node:assert/strict';
import { mkdtemp, mkdir, copyFile, writeFile, readFile, rm } from 'node:fs/promises';
import { tmpdir } from 'node:os';
import { join } from 'node:path';
import { spawnSync } from 'node:child_process';
const revision = 'a'.repeat(40);
async function fixture(t) {
 const root=await mkdtemp(join(tmpdir(),'fm-package-')); t.after(()=>rm(root,{recursive:true,force:true}));
 for(const name of ['scripts','deploy','bin','backend','frontend']) await mkdir(join(root,name));
 for(const name of ['scripts/package-demo.sh','compose.demo.yml','deploy/Caddyfile','deploy/.env.example'])
  await copyFile(new URL(`../${name}`,import.meta.url),join(root,name));
 await writeFile(join(root,'checks.json'),JSON.stringify({revision,approved:true,commands:['test: synthetic fixture']}));
 await writeFile(join(root,'bin/git'),'#!/bin/sh\ncase "$3" in status) printf "%s" "$TEST_DIRTY";; archive) tar -C "$2" -cf - compose.demo.yml deploy backend frontend;; rev-parse) printf "%s\\n" "'+revision+'";; esac\n',{mode:0o755});
 await writeFile(join(root,'bin/docker'),`#!/bin/sh
printf '%s\\n' "$*" >> "$TEST_DOCKER_LOG"
case "$1" in save) printf 'synthetic image archive';; image) printf '"sha256:fixture" []\\n';; esac
`,{mode:0o755});
 const env={...process.env,PATH:join(root,'bin')+':'+process.env.PATH,RELEASE_CHECKS_FILE:join(root,'checks.json'),TEST_DOCKER_LOG:join(root,'docker.log')};
 return {root,env,run:()=>spawnSync('bash',['scripts/package-demo.sh',join(root,'output')],{cwd:root,env,encoding:'utf8'})};
}
test('dirty checkout fails before invoking Docker',async t=>{
 const f=await fixture(t);f.env.TEST_DIRTY=' M source';const result=f.run();assert.notEqual(result.status,0);
 await assert.rejects(readFile(join(f.root,'docker.log')),{code:'ENOENT'});
});
test('checks must match full revision before build',async t=>{
 const f=await fixture(t);await writeFile(join(f.root,'checks.json'),JSON.stringify({revision:'wrong',approved:true,commands:['test']}));
 assert.notEqual(f.run().status,0);await assert.rejects(readFile(join(f.root,'docker.log')),{code:'ENOENT'});
});
test('clean package records full revision and exact image identities, verifies checksums, preserves preceding package',async t=>{
 const f=await fixture(t);const result=f.run();assert.equal(result.status,0,result.stderr);
 const manifest=JSON.parse(await readFile(join(f.root,'output/manifest.json'),'utf8'));
 assert.equal(manifest.revision,revision);assert.equal(manifest.platform,'linux/amd64');assert.equal(manifest.images.length,4);
 assert.equal(manifest.upstream_images.length,2);assert.ok(manifest.upstream_images.every(item=>item.includes('@sha256:')));
 const verify=spawnSync('sha256sum',['--check','--status','SHA256SUMS'],{cwd:join(f.root,'output')});assert.equal(verify.status,0);
 assert.notEqual(f.run().status,0);
 await writeFile(join(f.root,'output/RELEASE'),'tampered');
 assert.notEqual(spawnSync('sha256sum',['--check','--status','SHA256SUMS'],{cwd:join(f.root,'output')}).status,0);
});
