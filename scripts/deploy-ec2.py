#!/usr/bin/env python3
"""Operate the existing EC2 Compose installation; never provision infrastructure."""
import argparse
from contextlib import contextmanager
import fcntl
import json
import os
from pathlib import Path
import re
import subprocess
import sys
import time
import tempfile
import urllib.error
import urllib.request


class DeployError(Exception):
    pass


def command(args, timeout=360):
    try:
        return subprocess.run(args, check=True, capture_output=True, text=True,
                              timeout=timeout).stdout.strip()
    except (subprocess.SubprocessError, OSError) as exc:
        # Compose errors may include interpolated secrets. Keep them off CI logs.
        raise DeployError(f"Falha em {args[0]} {args[1] if len(args) > 1 else ''}; consulte os logs locais dos serviços") from exc


def atomic_json(path, value):
    temporary = path.with_suffix('.tmp')
    temporary.write_text(json.dumps(value, indent=2) + '\n')
    temporary.replace(path)


def validate_state(value):
    if not isinstance(value, dict):
        raise DeployError('Estado de deploy inválido')
    for key, pattern in [('revision', r'[0-9a-f]{40}'), ('migrations', r'[0-9a-f]{40}')]:
        if not isinstance(value.get(key), str) or not re.fullmatch(pattern, value[key]):
            raise DeployError(f'{key} inválido')
    if value.get('health_profile', 'ready') not in ('ready', 'legacy-accounts-v1'):
        raise DeployError('Perfil de saúde inválido')
    for key in ('backend', 'frontend'):
        # Bootstrap may capture local images from the original docker-load package.
        if not isinstance(value.get(key), str) or not re.fullmatch(r'(?:ghcr\.io/[a-z0-9_./-]+@)?sha256:[0-9a-f]{64}', value[key]):
            raise DeployError(f'Imagem imutável inválida: {key}')
    return value


class Deployment:
    def __init__(self, root):
        self.root = Path(root).resolve(strict=True)
        self.directory = self.root / '.deploy'
        self.directory.mkdir(mode=0o700, exist_ok=True)
        self.base = ['docker', 'compose', '--project-directory', str(self.root),
                     '--env-file', str(self.root / '.env.demo'), '-f', str(self.root / 'compose.demo.yml')]
        self.original_base = self.base.copy()

    def read(self, name):
        path = self.directory / f'{name}.json'
        return validate_state(json.loads(path.read_text())) if path.exists() else None

    def event(self, result, state):
        entry = {**state, 'result': result, 'time': time.strftime('%Y-%m-%dT%H:%M:%SZ', time.gmtime())}
        with (self.directory / 'events.jsonl').open('a') as stream:
            stream.write(json.dumps(entry) + '\n')
        print(json.dumps(entry), flush=True)

    def compose(self, state=None):
        # CLI --env-file values are overridden by process environment; do not modify .env.demo.
        os.environ.pop('COMPOSE_PROJECT_NAME', None)
        self.base = self.original_base.copy()
        if state:
            os.environ['BACKEND_IMAGE'] = state['backend']
            os.environ['FRONTEND_IMAGE'] = state['frontend']
            if state.get('health_profile') == 'legacy-accounts-v1':
                test = ["CMD-SHELL", "test \"$$(curl --silent --max-time 4 --output /dev/null --write-out '%{http_code}' http://localhost:8080/accounts)\" = 401"]
            else:
                test = ['CMD-SHELL', 'curl --fail --silent --max-time 4 http://localhost:8080/health/ready > /dev/null']
            override = self.directory / 'healthcheck.override.json'
            atomic_json(override, {'services': {'backend': {'healthcheck': {
                'test': test, 'interval': '10s', 'timeout': '5s', 'retries': 18, 'start_period': '60s'}}}})
            self.base += ['-f', str(override)]
        return self.base

    def config(self, state=None):
        config = json.loads(command(self.compose(state) + ['config', '--format', 'json']))
        services = config['services']
        if config.get('name') != 'finance-manager-demo' or not {'db', 'backend', 'frontend', 'proxy'} <= services.keys():
            raise DeployError('Instalação incompatível: confira projeto e serviços antes de adotar CD')
        if services['db'].get('ports'):
            raise DeployError('PostgreSQL não deve publicar portas nesta instalação')
        origin = services['backend']['environment']['CORS_ALLOWED_ORIGIN']
        if not re.fullmatch(r'https://[a-zA-Z0-9.-]+(?::[0-9]+)?', origin):
            raise DeployError('APP_ORIGIN deve ser uma origem HTTPS válida sem barra final')
        self.origin = origin
        return config

    def snapshot(self, revision, migrations):
        self.config()
        state = {'revision': revision, 'migrations': migrations}
        for service in ('backend', 'frontend'):
            cid = command(self.base + ['ps', '-q', service])
            if not cid or '\n' in cid:
                raise DeployError(f'Esperado exatamente um container: {service}')
            state[service] = command(['docker', 'inspect', '--format', '{{.Image}}', cid])
        return validate_state(state)

    def health(self, state):
        self.config(state)
        for service in ('db', 'backend', 'frontend', 'proxy'):
            cid = command(self.base + ['ps', '-q', service])
            if not cid or '\n' in cid:
                raise DeployError(f'Container ausente: {service}')
            status = command(['docker', 'inspect', '--format', '{{.State.Running}} {{.State.Health.Status}}', cid])
            if status != 'true healthy':
                raise DeployError(f'Serviço não saudável: {service}')
            if service in ('backend', 'frontend'):
                actual = command(['docker', 'inspect', '--format', '{{.Image}}', cid])
                expected = command(['docker', 'image', 'inspect', '--format', '{{.Id}}', state[service]])
                if actual != expected:
                    raise DeployError(f'Imagem não corresponde à entrega: {service}')
        # No redirects: a login redirect or HTTP redirect must not mask a broken endpoint.
        class NoRedirect(urllib.request.HTTPRedirectHandler):
            def redirect_request(self, req, fp, code, msg, headers, newurl):
                return None
        opener = urllib.request.build_opener(NoRedirect)
        if state.get('health_profile') == 'legacy-accounts-v1':
            # Explicit bootstrap/rollback profile for the inspected pre-readiness release.
            query = "SELECT EXISTS (SELECT 1 FROM pg_stat_activity WHERE datname=current_database() AND usename='finance_manager' AND application_name='PostgreSQL JDBC Driver')"
            connected = command(self.base + ['exec', '-T', 'db', 'psql', '-U', 'finance_manager',
                                           '-d', 'finance_manager', '-At', '-c', query], timeout=15)
            if connected != 't':
                raise DeployError('A versão legada não possui conexão JDBC ativa com o banco')
            for endpoint, expected in (('/login', 200), ('/api/accounts', 401)):
                try:
                    with opener.open(self.origin + endpoint, timeout=10) as response:
                        status = response.status
                except urllib.error.HTTPError as response:
                    status = response.code
                except OSError:
                    raise DeployError('Health check HTTPS legado falhou') from None
                if status != expected:
                    raise DeployError(f'Health check HTTPS legado falhou: {endpoint}')
            return
        for endpoint in ('/api/health/ready', '/api/health/live', '/login'):
            for attempt in range(6):
                try:
                    with opener.open(self.origin + endpoint, timeout=10) as response:
                        if response.status != 200:
                            raise ValueError('HTTP não 200')
                        if endpoint.startswith('/api/'):
                            payload = json.loads(response.read(4096))
                            if not isinstance(payload, dict) or payload.get('status') != 'UP':
                                raise ValueError('API indisponível')
                        elif not response.headers.get('Content-Type', '').startswith('text/html'):
                            raise ValueError('Frontend inválido')
                    break
                except (OSError, ValueError):
                    if attempt == 5:
                        raise DeployError(f'Health check HTTPS falhou: {endpoint}') from None
                    time.sleep(5)

    def apply(self, state, pull=False):
        self.config(state)
        if pull:
            command(self.base + ['pull', 'backend', 'frontend'])
        # --no-deps preserves PostgreSQL and Caddy, including their volumes/configuration.
        command(self.base + ['up', '-d', '--no-deps', '--pull', 'never', '--wait',
                             '--wait-timeout', '300', 'backend', 'frontend'])
        self.health(state)

    def run(self, args):
        current = self.read('current')
        if args.action == 'status':
            print(json.dumps({'current': current, 'previous': self.read('previous'),
                              'blocked': (self.directory / 'BLOCKED').exists()}, indent=2))
            return
        if (self.directory / 'BLOCKED').exists():
            raise DeployError('Recuperação pendente: consulte .deploy/BLOCKED e o runbook')
        if args.action == 'bootstrap':
            if current:
                raise DeployError('Bootstrap já realizado; estado preservado')
            state = self.snapshot(args.revision, args.migrations)
            if getattr(args, 'legacy_health', False):
                state['health_profile'] = 'legacy-accounts-v1'
            self.health(state)
            atomic_json(self.directory / 'current.json', state)
            self.event('bootstrap', state)
            return
        if not current:
            raise DeployError('Execute bootstrap após conferir a instalação existente')
        if args.action == 'rollback':
            target = self.read('previous')
            if not target or target['migrations'] != current['migrations']:
                raise DeployError('Rollback de schema não é automático; consulte o runbook')
        else:
            target = validate_state({'revision': args.revision, 'backend': args.backend,
                                     'frontend': args.frontend, 'migrations': args.migrations})
            if not all(target[key].startswith('ghcr.io/') for key in ('backend', 'frontend')):
                raise DeployError('Novas entregas devem usar digests GHCR')
        # Detect drift before trusting stored rollback images or adopting a new baseline.
        actual = self.snapshot(current['revision'], current['migrations'])
        for service in ('backend', 'frontend'):
            expected = command(['docker', 'image', 'inspect', '--format', '{{.Id}}', current[service]])
            if actual[service] != expected:
                raise DeployError('Imagens em execução divergem do estado registrado; investigue antes de continuar')
        self.health(current)
        if target == current:
            self.event('already-deployed', current)
            return
        changed_schema = target['migrations'] != current['migrations']
        approval = self.directory / 'approved-migrations' / target['revision']
        if changed_schema and not approval.is_file():
            raise DeployError('Migrations alteradas: backup e aprovação específica exigidos antes do deploy')
        self.config(target)
        # Download errors do not touch running containers or consume migration approval.
        if args.action == 'deploy':
            command(self.base + ['pull', 'backend', 'frontend'])
            for service in ('backend', 'frontend'):
                revision = command(['docker', 'image', 'inspect', '--format',
                                    '{{index .Config.Labels \"org.opencontainers.image.revision\"}}', target[service]])
                if revision != target['revision']:
                    raise DeployError('O digest baixado não corresponde à revisão aprovada')
        if changed_schema:
            approval.unlink()
        # Durable journal: interrupted SSH/runner cannot leave an unrecorded partial deployment.
        atomic_json(self.directory / 'pending.json', {'from': current, 'to': target})
        (self.directory / 'BLOCKED').write_text('Operação em andamento/interrompida. Inspecione pending.json antes de liberar.\n')
        self.event('starting', target)
        try:
            self.apply(target)
        except BaseException:
            self.event('failed', target)
            if changed_schema:
                self.event('recovery-required-schema', target)
                raise DeployError('Falha com migrations alteradas; recuperação assistida exigida') from None
            try:
                self.apply(current)
            except BaseException:
                self.event('rollback-failed', current)
                raise DeployError('Deploy e rollback falharam; recuperação assistida exigida') from None
            (self.directory / 'pending.json').unlink()
            (self.directory / 'BLOCKED').unlink()
            self.event('rolled-back', current)
            raise DeployError('Deploy falhou; versão anterior restaurada e validada') from None
        atomic_json(self.directory / 'previous.json', current)
        atomic_json(self.directory / 'current.json', target)
        (self.directory / 'pending.json').unlink()
        (self.directory / 'BLOCKED').unlink()
        self.event('deployed' if args.action == 'deploy' else 'manual-rollback', target)


@contextmanager
def registry_login(user):
    """Use a job-scoped GHCR read token; never modify the host's Docker login."""
    if not user:
        yield
        return
    if not re.fullmatch(r'[a-zA-Z0-9_.\[\]-]+', user):
        raise DeployError('Usuário do registry inválido')
    token = sys.stdin.readline(16385).strip()
    if not re.fullmatch(r'[a-zA-Z0-9_]{20,16384}', token):
        raise DeployError('Token temporário do registry inválido')
    original = os.environ.get('DOCKER_CONFIG')
    try:
        with tempfile.TemporaryDirectory(prefix='fm-registry-') as directory:
            os.environ['DOCKER_CONFIG'] = directory
            try:
                subprocess.run(['docker', 'login', 'ghcr.io', '--username', user, '--password-stdin'],
                               input=token + '\n', capture_output=True, text=True, timeout=30, check=True)
            except (subprocess.SubprocessError, OSError):
                raise DeployError('Login GHCR temporário falhou') from None
            yield
    finally:
        if original is None:
            os.environ.pop('DOCKER_CONFIG', None)
        else:
            os.environ['DOCKER_CONFIG'] = original


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('action', choices=['bootstrap', 'deploy', 'rollback', 'status'])
    parser.add_argument('root')
    parser.add_argument('--revision')
    parser.add_argument('--migrations')
    parser.add_argument('--backend')
    parser.add_argument('--frontend')
    parser.add_argument('--legacy-health', action='store_true', help='Somente bootstrap da versão sem endpoints de readiness')
    parser.add_argument('--registry-user')
    args = parser.parse_args()
    if args.action in ('bootstrap', 'deploy'):
        for key in ('revision', 'migrations'):
            if not re.fullmatch('[0-9a-f]{40}', getattr(args, key) or ''):
                parser.error(f'--{key} exige SHA completo')
    if args.legacy_health and args.action != 'bootstrap':
        parser.error('--legacy-health é permitido somente no bootstrap')
    if args.registry_user and args.action != 'deploy':
        parser.error('--registry-user é permitido somente no deploy')
    os.umask(0o077)
    try:
        deployment = Deployment(args.root)
        with registry_login(args.registry_user), (deployment.directory / 'lock').open('a') as lock:
            try:
                fcntl.flock(lock, fcntl.LOCK_EX | fcntl.LOCK_NB)
            except BlockingIOError:
                raise DeployError('Outra operação está em andamento') from None
            deployment.run(args)
    except (DeployError, OSError, ValueError, KeyError) as exc:
        # Do not print raw JSON/config/exception values, which can contain secrets.
        print(str(exc) if isinstance(exc, DeployError) else 'Falha de configuração/estado; consulte o runbook', file=sys.stderr)
        return 1
    return 0


if __name__ == '__main__':
    sys.exit(main())
