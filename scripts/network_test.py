import importlib.util
import io
import json
from pathlib import Path
import tempfile
import unittest
from unittest.mock import patch


def load(name, file):
    spec = importlib.util.spec_from_file_location(name, Path(__file__).with_name(file))
    module = importlib.util.module_from_spec(spec)
    spec.loader.exec_module(module)
    return module


network = load('network', 'deploy-network.py')
policies = load('policies', 'prepare-aws-oidc.py')
GROUP = 'sg-0123456789abcdef0'
OWNER = 'owner/repo:prod'
RUN = '123-1'


def rule(**changes):
    value = {'GroupId': GROUP, 'IsEgress': False, 'IpProtocol': 'tcp', 'FromPort': 22, 'ToPort': 22,
             'CidrIpv4': '203.0.113.4/32', 'SecurityGroupRuleId': 'sgr-fixture',
             'Tags': [{'Key': k, 'Value': v} for k, v in {
                 'ManagedBy': network.MANAGER, 'Owner': OWNER, 'GithubRun': RUN, 'ExpiresAt': '1'}.items()]}
    value.update(changes)
    return value


class NetworkTest(unittest.TestCase):
    def test_owned_requires_group_owner_tag_single_ipv4_and_ssh_only(self):
        self.assertTrue(network.owned(rule(), GROUP, OWNER))
        for changes in ({'GroupId': 'sg-other'}, {'IsEgress': True}, {'IpProtocol': '-1'},
                        {'FromPort': 80}, {'ToPort': 23}, {'CidrIpv4': '0.0.0.0/0'},
                        {'CidrIpv4': '203.0.113.0/24'}, {'CidrIpv4': '::/0'}, {'Tags': []}):
            with self.subTest(changes=changes):
                self.assertFalse(network.owned(rule(**changes), GROUP, OWNER))
        self.assertFalse(network.owned(rule(), GROUP, 'another/repo:prod'))

    def test_cleanup_preserves_administrator_rules_and_other_runs(self):
        another = rule()
        another['Tags'] = [dict(t, Value='456-1') if t['Key'] == 'GithubRun' else t for t in another['Tags']]
        with patch.object(network, 'rules', return_value=[rule(Tags=[]), another, rule()]), patch.object(network, 'aws') as aws:
            network.cleanup(GROUP, OWNER, RUN)
        aws.assert_called_once_with('revoke-security-group-ingress', '--group-id', GROUP,
                                    '--security-group-rule-ids', 'sgr-fixture')

    def test_sweep_preserves_unexpired_rules(self):
        future = rule()
        future['Tags'] = [dict(t, Value='9999999999') if t['Key'] == 'ExpiresAt' else t for t in future['Tags']]
        with patch.object(network, 'rules', return_value=[future, rule(Tags=[])]), patch.object(network, 'aws') as aws:
            network.cleanup(GROUP, OWNER)
            aws.assert_not_called()

    def test_sweep_revokes_expired_managed_rule(self):
        with patch.object(network, 'rules', return_value=[rule()]), patch.object(network, 'aws') as aws:
            network.cleanup(GROUP, OWNER)
            self.assertEqual(aws.call_args.args[0], 'revoke-security-group-ingress')

    def test_open_records_intent_before_api_and_limits_port_cidr(self):
        response = io.BytesIO(b'8.8.8.8\n')
        with tempfile.TemporaryDirectory() as directory:
            state = Path(directory) / 'state.json'
            def aws(*args):
                self.assertTrue(state.exists())
                self.assertEqual(json.loads(state.read_text())['run'], RUN)
                permission = json.loads(args[args.index('--ip-permissions') + 1])[0]
                self.assertEqual((permission['FromPort'], permission['ToPort'], permission['IpProtocol']), (22, 22, 'tcp'))
                self.assertEqual(permission['IpRanges'][0]['CidrIp'], '8.8.8.8/32')
                tags = json.loads(args[args.index('--tag-specifications') + 1])[0]['Tags']
                self.assertEqual({t['Key']: t['Value'] for t in tags}['GithubRun'], RUN)
                return {}
            with patch.object(network, 'cleanup'), patch.object(network.urllib.request, 'urlopen', return_value=response), \
                 patch.object(network, 'aws', side_effect=aws), patch.object(network, 'rules', return_value=[rule()]):
                network.open_access(GROUP, OWNER, RUN, state)

    def test_duplicate_does_not_revoke_existing_rule(self):
        with tempfile.TemporaryDirectory() as directory, patch.object(network, 'cleanup') as cleanup, \
             patch.object(network.urllib.request, 'urlopen', return_value=io.BytesIO(b'8.8.8.8')), \
             patch.object(network, 'aws', side_effect=FileExistsError('duplicate')) as aws:
            network.open_access(GROUP, OWNER, RUN, Path(directory) / 'state')
            cleanup.assert_called_once_with(GROUP, OWNER)
            self.assertEqual(aws.call_count, 1)

    def test_private_ip_never_authorizes_rule(self):
        with tempfile.TemporaryDirectory() as directory, patch.object(network, 'cleanup'), \
             patch.object(network.urllib.request, 'urlopen', return_value=io.BytesIO(b'127.0.0.1')), \
             patch.object(network, 'aws') as aws:
            with self.assertRaises(ValueError):
                network.open_access(GROUP, OWNER, RUN, Path(directory) / 'state')
            aws.assert_not_called()

    def test_revoke_is_idempotent_if_another_sweeper_already_deleted_rule(self):
        result = unittest.mock.Mock(returncode=255, stderr='InvalidSecurityGroupRuleId.NotFound', stdout='')
        with patch.object(network.subprocess, 'run', return_value=result):
            self.assertEqual(network.aws('revoke-security-group-ingress'), {})

    def test_policy_subject_exact_environment_and_scope_one_group(self):
        subject = 'repo:owner@123/repo@456:environment:prod'
        trust, permissions = policies.documents('123456789012', 'us-east-1', GROUP, subject)
        condition = trust['Statement'][0]['Condition']['StringEquals']
        self.assertEqual(condition['token.actions.githubusercontent.com:sub'], subject)
        self.assertEqual(condition['token.actions.githubusercontent.com:aud'], 'sts.amazonaws.com')
        self.assertEqual(permissions['Statement'][0]['Resource'], f'arn:aws:ec2:us-east-1:123456789012:security-group/{GROUP}')
        self.assertNotIn('iam:', json.dumps(permissions))
        self.assertNotIn('ec2:*', json.dumps(permissions))

    def test_policy_rejects_wildcard_and_branch_subject(self):
        for subject in ('repo:owner/repo:*', 'repo:owner@123/repo@456:ref:refs/heads/main', '*'):
            with self.assertRaises(ValueError):
                policies.documents('123456789012', 'us-east-1', GROUP, subject)


if __name__ == '__main__':
    unittest.main()
