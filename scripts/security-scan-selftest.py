#!/usr/bin/env python3
"""Exercise the real scanner with disposable synthetic data, never live credentials."""
import base64
import json
from pathlib import Path
import secrets
import shutil
import string
import subprocess
import tempfile

ROOT = Path(__file__).resolve().parents[1]
CONFIG = ROOT / '.gitleaks.toml'
SCANNER = shutil.which('gitleaks')
if not SCANNER:
    raise SystemExit('Instale o Gitleaks fixado na CI e inclua-o no PATH.')


def scan(directory, expected, mode='dir'):
    report = directory.parent / 'report.json'
    result = subprocess.run(
        [SCANNER, mode, '--config', str(CONFIG), '--redact=100', '--no-banner',
         '--report-format=json', '--report-path', str(report),
         *(['--log-opts=--all'] if mode == 'git' else []), '.'],
        cwd=directory, capture_output=True, text=True,
    )
    # Do not print scanner output: even synthetic test reports stay temporary.
    if result.returncode != expected:
        raise SystemExit(f'Autoteste falhou: esperado {expected}, recebido {result.returncode}.')
    findings = json.loads(report.read_text())
    if expected == 1 and not any(f['RuleID'] == 'generic-api-key' for f in findings):
        raise SystemExit('Autoteste falhou: regra generic-api-key não detectou a fixture.')


with tempfile.TemporaryDirectory(prefix='fm-secret-selftest-') as temporary:
    directory = Path(temporary) / 'source'
    directory.mkdir()
    jwt_path = directory / 'backend/src/test/java/io/github/gabrielhdias/financeManager/config/security/JwtRealMvcTest.java'
    jwt_path.parent.mkdir(parents=True)
    jwt_fixture = base64.b64encode(b'0123456789abcdef' * 2).decode()
    jwt_path.write_text(f'security.jwt.secret={jwt_fixture}\n')
    comment_path = directory / 'scripts/deploy-ec2.py'
    comment_path.parent.mkdir(parents=True)
    comment_path.write_text('# GitHub cred' + 'entials are opaque: punctuation/JWT separators are valid password bytes.\n')
    scan(directory, 0)

    # The same fixture in a different path must not inherit the exception.
    other = directory / 'unexpected.txt'
    other.write_text(f'security.jwt.secret={jwt_fixture}\n')
    scan(directory, 1)
    other.write_text(comment_path.read_text())
    scan(directory, 1)
    other.unlink()

    # New values inside the allowed test file must still fail.
    synthetic = ''.join(secrets.choice(string.ascii_letters + string.digits) for _ in range(48))
    jwt_path.write_text(f'api_key="{synthetic}"\n')
    scan(directory, 1)
    jwt_path.unlink()
    # Appending a key to the exempt comment must also fail.
    comment_path.write_text(comment_path.read_text().rstrip() + f' api_key="{synthetic}"\n')
    scan(directory, 1)
    comment_path.unlink()

    # A deleted secret must remain detectable in Git history.
    subprocess.run(['git', 'init', '-q', str(directory)], check=True)
    other.write_text(f'api_key="{synthetic}"\n')
    for delete in (False, True):
        if delete:
            other.unlink()
        subprocess.run(['git', '-C', str(directory), 'add', '-A'], check=True)
        subprocess.run(['git', '-C', str(directory), '-c', 'user.name=Scanner Selftest',
                        '-c', 'user.email=scanner@example.invalid', '-c', 'commit.gpgsign=false',
                        'commit', '-qm', 'Synthetic scanner test'], check=True)
    scan(directory, 1, mode='git')
    scan(directory, 0)

print('Gitleaks: detecção, histórico e limites das duas exceções validados; fixtures removidas.')
