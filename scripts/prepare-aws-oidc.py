#!/usr/bin/env python3
"""Generate reviewable IAM documents; this script never calls AWS or changes a role."""
import argparse
import json
from pathlib import Path
import re


def documents(account, region, group, subject):
    if not re.fullmatch(r'[0-9]{12}', account) or not re.fullmatch(r'[a-z]{2}-[a-z]+-[1-9]', region):
        raise ValueError('Conta/região inválidas')
    if not re.fullmatch(r'sg-[0-9a-f]{8,17}', group):
        raise ValueError('Security Group inválido')
    if not re.fullmatch(r'repo:[a-zA-Z0-9_.-]+@[0-9]+/[a-zA-Z0-9_.-]+@[0-9]+:environment:prod', subject):
        raise ValueError('Use o subject imutável exato do repositório e Environment prod')
    trust = {'Version': '2012-10-17', 'Statement': [{
        'Effect': 'Allow', 'Principal': {'Federated': f'arn:aws:iam::{account}:oidc-provider/token.actions.githubusercontent.com'},
        'Action': 'sts:AssumeRoleWithWebIdentity', 'Condition': {'StringEquals': {
            'token.actions.githubusercontent.com:aud': 'sts.amazonaws.com',
            'token.actions.githubusercontent.com:sub': subject}}}]}
    permissions = {'Version': '2012-10-17', 'Statement': [
        {'Sid': 'ManageIngressOnlyOnDeploymentGroup', 'Effect': 'Allow',
         'Action': ['ec2:AuthorizeSecurityGroupIngress', 'ec2:RevokeSecurityGroupIngress'],
         'Resource': f'arn:aws:ec2:{region}:{account}:security-group/{group}'},
        {'Sid': 'AuthorizeTaggedRules', 'Effect': 'Allow',
         'Action': 'ec2:AuthorizeSecurityGroupIngress',
         'Resource': f'arn:aws:ec2:{region}:{account}:security-group-rule/*',
         'Condition': {'StringEquals': {'aws:RequestTag/ManagedBy': 'finance-manager-cd'}}},
        {'Sid': 'ReadRulesInDeploymentRegion', 'Effect': 'Allow',
         'Action': 'ec2:DescribeSecurityGroupRules', 'Resource': '*',
         'Condition': {'StringEquals': {'aws:RequestedRegion': region}}},
        {'Sid': 'TagOnlyWhileAuthorizingIngress', 'Effect': 'Allow', 'Action': 'ec2:CreateTags',
         'Resource': f'arn:aws:ec2:{region}:{account}:security-group-rule/*',
         'Condition': {'StringEquals': {'ec2:CreateAction': 'AuthorizeSecurityGroupIngress',
                                      'aws:RequestTag/ManagedBy': 'finance-manager-cd'},
                       'ForAllValues:StringEquals': {'aws:TagKeys': ['ManagedBy', 'Owner', 'GithubRun', 'ExpiresAt']}}}
    ]}
    return trust, permissions


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    for name in ('account', 'region', 'group', 'subject', 'output'):
        parser.add_argument('--' + name, required=True)
    args = parser.parse_args()
    trust, permissions = documents(args.account, args.region, args.group, args.subject)
    output = Path(args.output)
    output.mkdir(mode=0o700, parents=True, exist_ok=True)
    for name, data in [('trust.json', trust), ('permissions.json', permissions)]:
        path = output / name
        path.write_text(json.dumps(data, indent=2) + '\n')
        print(path)


if __name__ == '__main__':
    main()
