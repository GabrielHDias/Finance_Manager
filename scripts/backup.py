#!/usr/bin/env python3
"""Local PostgreSQL backup tooling. No scheduling or infrastructure mutations."""
import argparse
import hashlib
import json
import os
from pathlib import Path
import subprocess
import tempfile
import time
import uuid


def run(args, **kwargs):
    # Never echo commands, database output or environment on failure.
    result = subprocess.run(args, stderr=subprocess.DEVNULL, **kwargs)
    if result.returncode:
        raise RuntimeError(f'{Path(args[0]).name} falhou (código {result.returncode})')


def checksum(path):
    digest = hashlib.sha256()
    with path.open('rb') as stream:
        for block in iter(lambda: stream.read(1024 * 1024), b''):
            digest.update(block)
    return digest.hexdigest()


def verify(metadata, max_age_hours):
    info = json.loads(metadata.read_text())
    filename = info['file']
    if Path(filename).name != filename:
        raise ValueError('Nome de backup inválido')
    archive = metadata.parent / filename
    if archive.stat().st_size == 0 or checksum(archive) != info['sha256']:
        raise ValueError('Backup vazio ou checksum inválido')
    age = time.time() - info['created_at']
    if not 0 <= age <= max_age_hours * 3600:
        raise ValueError('Backup ausente, futuro ou antigo demais')
    return archive


def backup(output, recipient, retention_days):
    if retention_days < 1:
        raise ValueError('Retenção deve ser positiva')
    output.mkdir(mode=0o700, parents=True, exist_ok=True)
    if output.stat().st_mode & 0o077:
        raise ValueError('Diretório do backup deve ter permissão 0700')
    created = time.time()
    name = f'backup-{int(created)}-{uuid.uuid4().hex}.dump.age'
    archive = output / name
    with tempfile.TemporaryDirectory(prefix='.backup-', dir=output) as folder:
        plain = Path(folder) / 'database.dump'
        with plain.open('xb') as stream:
            run(['pg_dump', '--format=custom', '--no-owner', '--no-acl'], stdout=stream)
        if plain.stat().st_size == 0:
            raise ValueError('Dump vazio')
        run(['pg_restore', '--list', str(plain)], stdout=subprocess.DEVNULL)
        encrypted = Path(folder) / 'encrypted'
        run(['age', '--recipient', recipient, '--output', str(encrypted), str(plain)])
        if encrypted.stat().st_size == 0:
            raise ValueError('Arquivo criptografado vazio')
        os.replace(encrypted, archive)
    metadata = archive.with_suffix(archive.suffix + '.json')
    metadata.write_text(json.dumps({'file': name, 'sha256': checksum(archive), 'created_at': created,
                                    'format': 'pg_dump-custom+age', 'version': 1}) + '\n')
    # Delete only verified tool-owned pairs, after the new backup succeeds.
    for old in output.glob('backup-*.dump.age.json'):
        try:
            info = json.loads(old.read_text())
            if info.get('version') == 1 and created - info['created_at'] > retention_days * 86400:
                candidate = verify(old, 24 * 365 * 100)
                candidate.unlink()
                old.unlink()
        except (ValueError, KeyError, OSError):
            pass
    return metadata


def restore(metadata, identity, database, max_age_hours):
    # Restoration is deliberately restricted to a separate disposable local DB.
    if not database.startswith('fm_restore_') or not database.replace('_', '').isalnum():
        raise ValueError('Use um banco descartável com prefixo fm_restore_')
    if os.environ.get('PGHOST') not in ('127.0.0.1', 'localhost', '::1'):
        raise ValueError('Restauração requer PGHOST local explícito')
    archive = verify(metadata, max_age_hours)
    with tempfile.TemporaryDirectory(prefix='fm-restore-') as folder:
        plain = Path(folder) / 'database.dump'
        run(['age', '--decrypt', '--identity', str(identity), '--output', str(plain), str(archive)])
        run(['pg_restore', '--list', str(plain)], stdout=subprocess.DEVNULL)
        # No --clean or --create: an occupied destination fails atomically.
        run(['pg_restore', '--exit-on-error', '--single-transaction', '--no-owner', '--no-acl',
             '--dbname', database, str(plain)], stdout=subprocess.DEVNULL)


def main():
    os.umask(0o077)
    parser = argparse.ArgumentParser(description=__doc__)
    sub = parser.add_subparsers(dest='command', required=True)
    create = sub.add_parser('backup')
    create.add_argument('--output', type=Path, required=True)
    create.add_argument('--recipient', required=True)
    create.add_argument('--retention-days', type=int, default=30)
    for command in ('verify', 'restore'):
        cmd = sub.add_parser(command)
        cmd.add_argument('--metadata', type=Path, required=True)
        cmd.add_argument('--max-age-hours', type=float, default=26)
        if command == 'restore':
            cmd.add_argument('--identity', type=Path, required=True)
            cmd.add_argument('--database', required=True)
    args = parser.parse_args()
    try:
        if args.command == 'backup':
            metadata = backup(args.output, args.recipient, args.retention_days)
            print(f'Backup criptografado validado: {metadata.name}')
        elif args.command == 'verify':
            verify(args.metadata, args.max_age_hours)
            print('Integridade e idade do backup válidas')
        else:
            restore(args.metadata, args.identity, args.database, args.max_age_hours)
            print('Restauração concluída no destino descartável; valide migrações e totais')
    except (ValueError, KeyError, OSError, RuntimeError):
        parser.exit(1, 'Falha na operação de backup; verifique configuração e ferramentas sem expor dados.\n')


if __name__ == '__main__':
    main()
