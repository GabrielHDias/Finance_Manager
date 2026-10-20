#!/usr/bin/python3
"""Root-owned forced-command gateway for the dedicated SSH deploy key."""
import json
import os
from pathlib import Path
import re
import shlex
import sys


def validate_request(original, config):
    args = shlex.split(original)
    root = config['root']
    if len(args) < 4 or args[:2] != ['python3', root + '/deploy-cd.py'] or args[3] != root:
        raise ValueError('Comando fora do contrato de deploy')
    if args[2] == 'status' and len(args) == 4:
        return args
    if args[2] != 'deploy' or (len(args) - 4) % 2:
        raise ValueError('Ação não permitida')
    options = {}
    for key, value in zip(args[4::2], args[5::2]):
        if key in options:
            raise ValueError('Opção repetida')
        options[key] = value
    required = {'--revision', '--migrations', '--backend', '--frontend'}
    if not required <= options.keys() or options.keys() - required - {'--registry-user'}:
        raise ValueError('Opções inválidas')
    for key in ('--revision', '--migrations'):
        if not re.fullmatch(r'[0-9a-f]{40}', options[key]):
            raise ValueError('SHA inválido')
    for service in ('backend', 'frontend'):
        if not re.fullmatch(re.escape(config['registry'] + '-' + service) + r'@sha256:[0-9a-f]{64}', options['--' + service]):
            raise ValueError('Imagem fora do repositório permitido')
    if '--registry-user' in options and not re.fullmatch(r'[a-zA-Z0-9_.\[\]-]+', options['--registry-user']):
        raise ValueError('Usuário do registry inválido')
    return args


def main():
    try:
        config = json.loads(Path('/etc/finance-manager-deploy.json').read_text())
        args = validate_request(os.environ.get('SSH_ORIGINAL_COMMAND', ''), config)
        # No shell evaluation. SSH stdin is inherited for the short-lived registry token.
        os.execv('/usr/bin/python3', args)
    except (OSError, ValueError, KeyError):
        print('Comando não permitido pela chave exclusiva de deploy', file=sys.stderr)
        return 1


if __name__ == '__main__':
    sys.exit(main())
