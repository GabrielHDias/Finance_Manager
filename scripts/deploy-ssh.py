#!/usr/bin/env python3
"""SSH transport for CI; production secrets remain on EC2."""
import os
from pathlib import Path
import re
import shlex
import subprocess
import tempfile


def main():
    env = {key: os.environ.get(key, '') for key in (
        'DEPLOY_HOST', 'DEPLOY_USER', 'DEPLOY_PORT', 'DEPLOY_PATH', 'DEPLOY_SSH_KEY',
        'DEPLOY_KNOWN_HOSTS', 'RELEASE_SHA', 'MIGRATIONS_SHA', 'BACKEND_IMAGE', 'FRONTEND_IMAGE',
        'DEPLOY_ACTION', 'DEPLOY_REGISTRY_USER', 'DEPLOY_REGISTRY_TOKEN')}
    action = env['DEPLOY_ACTION'] or 'deploy'
    if action not in ('deploy', 'status'):
        raise SystemExit('Ação SSH inválida')
    patterns = {
        'DEPLOY_HOST': r'[a-zA-Z0-9][a-zA-Z0-9.-]*',
        'DEPLOY_USER': r'[a-z_][a-z0-9_-]*',
        'DEPLOY_PORT': r'[0-9]{1,5}',
        'DEPLOY_PATH': r'/[a-zA-Z0-9_./-]+',
    }
    if action == 'deploy':
        patterns.update({
        'RELEASE_SHA': r'[0-9a-f]{40}', 'MIGRATIONS_SHA': r'[0-9a-f]{40}',
        'BACKEND_IMAGE': r'ghcr\.io/[a-z0-9_./-]+@sha256:[0-9a-f]{64}',
        'FRONTEND_IMAGE': r'ghcr\.io/[a-z0-9_./-]+@sha256:[0-9a-f]{64}',
        })
    for key, pattern in patterns.items():
        if not re.fullmatch(pattern, env[key]):
            raise SystemExit(f'Configuração inválida: {key}')
    if not 1 <= int(env['DEPLOY_PORT']) <= 65535:
        raise SystemExit('Porta SSH inválida')
    if not env['DEPLOY_SSH_KEY'] or not env['DEPLOY_KNOWN_HOSTS']:
        raise SystemExit('Chave exclusiva e known_hosts confiável são obrigatórios')
    user, token = env['DEPLOY_REGISTRY_USER'], env['DEPLOY_REGISTRY_TOKEN']
    if bool(user) != bool(token) or (user and (action != 'deploy' or not re.fullmatch(r'[a-zA-Z0-9_.\[\]-]+', user))):
        raise SystemExit('Credenciais temporárias do registry inválidas')
    os.umask(0o077)
    with tempfile.TemporaryDirectory(prefix='fm-ssh-') as directory:
        key = Path(directory) / 'key'
        hosts = Path(directory) / 'known_hosts'
        key.write_text(env['DEPLOY_SSH_KEY'] + '\n')
        hosts.write_text(env['DEPLOY_KNOWN_HOSTS'] + '\n')
        root = env['DEPLOY_PATH'].rstrip('/')
        remote_args = ['python3', root + '/deploy-cd.py', action, root]
        if action == 'deploy':
            remote_args += ['--revision', env['RELEASE_SHA'], '--migrations', env['MIGRATIONS_SHA'],
                            '--backend', env['BACKEND_IMAGE'], '--frontend', env['FRONTEND_IMAGE']]
        if user:
            remote_args += ['--registry-user', user]
        remote = shlex.join(remote_args)
        transport = {'input': token + '\n', 'text': True} if token else {'stdin': subprocess.DEVNULL}
        subprocess.run(['ssh', '-F', '/dev/null', '-T', '-i', str(key), '-p', env['DEPLOY_PORT'],
                        '-o', 'StrictHostKeyChecking=yes', '-o', f'UserKnownHostsFile={hosts}',
                        '-o', 'BatchMode=yes', '-o', 'PasswordAuthentication=no',
                        '-o', 'KbdInteractiveAuthentication=no', '-o', 'IdentitiesOnly=yes',
                        '-o', 'ConnectTimeout=15', '-o', 'ServerAliveInterval=15',
                        '-o', 'ServerAliveCountMax=4',
                        f"{env['DEPLOY_USER']}@{env['DEPLOY_HOST']}", remote],
                       **transport, check=True, timeout=1200)


if __name__ == '__main__':
    main()
