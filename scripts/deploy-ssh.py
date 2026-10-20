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
        'DEPLOY_KNOWN_HOSTS', 'RELEASE_SHA', 'MIGRATIONS_SHA', 'BACKEND_IMAGE', 'FRONTEND_IMAGE')}
    for key, pattern in {
        'DEPLOY_HOST': r'[a-zA-Z0-9][a-zA-Z0-9.-]*',
        'DEPLOY_USER': r'[a-z_][a-z0-9_-]*',
        'DEPLOY_PORT': r'[0-9]{1,5}',
        'DEPLOY_PATH': r'/[a-zA-Z0-9_./-]+',
        'RELEASE_SHA': r'[0-9a-f]{40}', 'MIGRATIONS_SHA': r'[0-9a-f]{40}',
        'BACKEND_IMAGE': r'ghcr\.io/[a-z0-9_./-]+@sha256:[0-9a-f]{64}',
        'FRONTEND_IMAGE': r'ghcr\.io/[a-z0-9_./-]+@sha256:[0-9a-f]{64}',
    }.items():
        if not re.fullmatch(pattern, env[key]):
            raise SystemExit(f'Configuração inválida: {key}')
    if not 1 <= int(env['DEPLOY_PORT']) <= 65535:
        raise SystemExit('Porta SSH inválida')
    if not env['DEPLOY_SSH_KEY'] or not env['DEPLOY_KNOWN_HOSTS']:
        raise SystemExit('Chave exclusiva e known_hosts confiável são obrigatórios')
    os.umask(0o077)
    with tempfile.TemporaryDirectory(prefix='fm-ssh-') as directory:
        key = Path(directory) / 'key'
        hosts = Path(directory) / 'known_hosts'
        key.write_text(env['DEPLOY_SSH_KEY'] + '\n')
        hosts.write_text(env['DEPLOY_KNOWN_HOSTS'] + '\n')
        root = env['DEPLOY_PATH'].rstrip('/')
        remote = shlex.join(['python3', root + '/deploy-cd.py', 'deploy', root,
                             '--revision', env['RELEASE_SHA'], '--migrations', env['MIGRATIONS_SHA'],
                             '--backend', env['BACKEND_IMAGE'], '--frontend', env['FRONTEND_IMAGE']])
        subprocess.run(['ssh', '-F', '/dev/null', '-T', '-i', str(key), '-p', env['DEPLOY_PORT'],
                        '-o', 'StrictHostKeyChecking=yes', '-o', f'UserKnownHostsFile={hosts}',
                        '-o', 'BatchMode=yes', '-o', 'PasswordAuthentication=no',
                        '-o', 'KbdInteractiveAuthentication=no', '-o', 'IdentitiesOnly=yes',
                        '-o', 'ConnectTimeout=15', '-o', 'ServerAliveInterval=15',
                        '-o', 'ServerAliveCountMax=4',
                        f"{env['DEPLOY_USER']}@{env['DEPLOY_HOST']}", remote],
                       stdin=subprocess.DEVNULL, check=True, timeout=1200)


if __name__ == '__main__':
    main()
