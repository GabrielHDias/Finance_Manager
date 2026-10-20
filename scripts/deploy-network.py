#!/usr/bin/env python3
"""Temporarily authorize the GitHub runner's single IPv4 on SSH, using AWS CLI/OIDC."""
import argparse
import ipaddress
import json
import os
from pathlib import Path
import re
import subprocess
import sys
import time
import urllib.request

MANAGER = 'finance-manager-cd'


def aws(*args):
    result = subprocess.run(['aws', 'ec2', *args, '--output', 'json', '--no-cli-pager'],
                            capture_output=True, text=True, timeout=45)
    if result.returncode:
        if args[0] == 'revoke-security-group-ingress' and any(code in result.stderr for code in ('InvalidSecurityGroupRuleId.NotFound', 'InvalidPermission.NotFound')):
            return {}
        if 'InvalidPermission.Duplicate' in result.stderr:
            raise FileExistsError('A regra SSH deste IP já existe; a regra existente será preservada')
        raise RuntimeError('AWS EC2 falhou; confira a role, região e o Security Group no CloudTrail')
    return json.loads(result.stdout or '{}')


def rules(group):
    return aws('describe-security-group-rules', '--filters', f'Name=group-id,Values={group}')['SecurityGroupRules']


def owned(rule, group, owner):
    tags = {item['Key']: item['Value'] for item in rule.get('Tags', [])}
    try:
        network = ipaddress.ip_network(rule.get('CidrIpv4', ''))
    except ValueError:
        return False
    return (rule.get('GroupId') == group and not rule.get('IsEgress', True)
            and rule.get('IpProtocol') == 'tcp' and rule.get('FromPort') == 22
            and rule.get('ToPort') == 22 and network.version == 4 and network.prefixlen == 32
            and tags.get('ManagedBy') == MANAGER and tags.get('Owner') == owner)


def cleanup(group, owner, run=None):
    """Revoke this run, or expired leases only. Never revoke administrator rules."""
    for rule in rules(group):
        if not owned(rule, group, owner):
            continue
        tags = {item['Key']: item['Value'] for item in rule['Tags']}
        if run is not None:
            selected = tags.get('GithubRun') == run
        else:
            expires = tags.get('ExpiresAt', '')
            selected = expires.isdigit() and int(expires) < int(time.time())
        if selected:
            aws('revoke-security-group-ingress', '--group-id', group,
                '--security-group-rule-ids', rule['SecurityGroupRuleId'])
            print('Regra temporária revogada:', rule['SecurityGroupRuleId'], flush=True)


def open_access(group, owner, run, state):
    cleanup(group, owner)  # Recover tagged, expired leases left by a hard runner failure.
    with urllib.request.urlopen('https://checkip.amazonaws.com', timeout=10) as response:
        address = ipaddress.ip_address(response.read(128).decode().strip())
    if address.version != 4 or not address.is_global:
        raise ValueError('Esperado um único IPv4 público do runner')
    cidr = f'{address}/32'
    # Save before mutation, so the always() step can recover even a lost API response.
    state.write_text(json.dumps({'group': group, 'owner': owner, 'run': run, 'cidr': cidr}))
    permissions = [{'IpProtocol': 'tcp', 'FromPort': 22, 'ToPort': 22,
                    'IpRanges': [{'CidrIp': cidr, 'Description': f'{MANAGER}:{run}'}]}]
    tags = [{'ResourceType': 'security-group-rule', 'Tags': [
        {'Key': 'ManagedBy', 'Value': MANAGER}, {'Key': 'Owner', 'Value': owner},
        {'Key': 'GithubRun', 'Value': run}, {'Key': 'ExpiresAt', 'Value': str(int(time.time()) + 3600)}]}]
    try:
        aws('authorize-security-group-ingress', '--group-id', group,
            '--ip-permissions', json.dumps(permissions), '--tag-specifications', json.dumps(tags))
    except FileExistsError as exc:
        # Do not take ownership of an existing manually managed rule.
        print(str(exc), flush=True)
        return
    print(f'SSH TCP 22 liberado temporariamente para {cidr}', flush=True)
    for attempt in range(6):
        if any(owned(rule, group, owner)
               and {t['Key']: t['Value'] for t in rule.get('Tags', [])}.get('GithubRun') == run
               for rule in rules(group)):
            return
        time.sleep(2)
    raise RuntimeError('A regra temporária não foi confirmada')


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('action', choices=['open', 'close', 'sweep'])
    args = parser.parse_args()
    group = os.environ.get('DEPLOY_SECURITY_GROUP_ID', '')
    region = os.environ.get('AWS_REGION', '')
    repository = os.environ.get('GITHUB_REPOSITORY', '')
    run = f"{os.environ.get('GITHUB_RUN_ID', '')}-{os.environ.get('GITHUB_RUN_ATTEMPT', '')}"
    if not re.fullmatch(r'sg-[0-9a-f]{8,17}', group) or not re.fullmatch(r'[a-z]{2}-[a-z]+-[1-9]', region):
        parser.error('Security Group/região inválidos')
    if not re.fullmatch(r'[a-zA-Z0-9_.-]+/[a-zA-Z0-9_.-]+', repository) or not re.fullmatch(r'[0-9]+-[0-9]+', run):
        parser.error('Identidade da execução GitHub inválida')
    owner = repository + ':prod'
    state = Path(os.environ['RUNNER_TEMP']) / 'finance-manager-network.json'
    try:
        if args.action == 'open':
            open_access(group, owner, run, state)
        elif args.action == 'close':
            # Also query by run tags if the API accepted a request but the runner lost its response.
            for attempt in range(3):
                try:
                    cleanup(group, owner, run)
                    remaining = [r for r in rules(group) if owned(r, group, owner)
                                 and {t['Key']: t['Value'] for t in r.get('Tags', [])}.get('GithubRun') == run]
                    if remaining:
                        raise RuntimeError('Regra temporária ainda presente')
                    state.unlink(missing_ok=True)
                    return 0
                except (RuntimeError, subprocess.SubprocessError):
                    if attempt == 2:
                        raise
                    time.sleep(3)
        else:
            cleanup(group, owner)
    except (OSError, ValueError, RuntimeError, subprocess.SubprocessError):
        print('Falha na gestão da regra temporária; confira o Security Group e CloudTrail. '
              'Uma falha de limpeza exige revogação operacional.', file=sys.stderr)
        return 1
    return 0


if __name__ == '__main__':
    sys.exit(main())
