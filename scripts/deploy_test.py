"""Failure-oriented tests; Docker, HTTPS and SSH never touch real infrastructure."""
import argparse
import importlib.util
import json
import os
from pathlib import Path
import subprocess
import tempfile
import unittest
from unittest.mock import patch

spec = importlib.util.spec_from_file_location('deploy', Path(__file__).with_name('deploy-ec2.py'))
deploy = importlib.util.module_from_spec(spec)
spec.loader.exec_module(deploy)
OLD = {'revision': 'a' * 40, 'migrations': 'b' * 40,
       'backend': 'sha256:' + 'c' * 64, 'frontend': 'sha256:' + 'd' * 64}
NEW = {'revision': 'e' * 40, 'migrations': 'b' * 40,
       'backend': 'ghcr.io/owner/backend@sha256:' + 'f' * 64,
       'frontend': 'ghcr.io/owner/frontend@sha256:' + '1' * 64}


class DeployTest(unittest.TestCase):
    def setUp(self):
        self.tmp = tempfile.TemporaryDirectory()
        self.addCleanup(self.tmp.cleanup)
        self.environment = patch.dict(os.environ, {}, clear=False)
        self.environment.start()
        self.addCleanup(self.environment.stop)
        self.app = deploy.Deployment(self.tmp.name)
        deploy.atomic_json(self.app.directory / 'current.json', OLD)
        self.args = argparse.Namespace(action='deploy', **NEW)

    def run_update(self, failure=None, target=None, action='deploy'):
        self.args.action = action
        if target:
            self.args = argparse.Namespace(action=action, **target)
        with patch.object(self.app, 'snapshot', return_value=OLD), \
             patch.object(self.app, 'health') as health, \
             patch.object(self.app, 'config'), \
             patch.object(self.app, 'apply', side_effect=failure) as apply, \
             patch.object(deploy, 'command', side_effect=lambda args: NEW['revision'] if any('org.opencontainers.image.revision' in a for a in args) else (OLD['backend'] if args[-1] == OLD['backend'] else OLD['frontend'])):
            self.app.run(self.args)
            return apply, health

    def test_success_commits_both_images_and_retains_previous(self):
        apply, _ = self.run_update()
        apply.assert_called_once_with(NEW)
        self.assertEqual(self.app.read('current'), NEW)
        self.assertEqual(self.app.read('previous'), OLD)
        self.assertFalse((self.app.directory / 'BLOCKED').exists())
        self.assertFalse((self.app.directory / 'pending.json').exists())
        self.assertIn('deployed', (self.app.directory / 'events.jsonl').read_text())

    def test_same_release_is_idempotent_and_preserves_rollback(self):
        deploy.atomic_json(self.app.directory / 'current.json', NEW)
        deploy.atomic_json(self.app.directory / 'previous.json', OLD)
        with patch.object(self.app, 'snapshot', return_value=NEW), \
             patch.object(self.app, 'health'), patch.object(self.app, 'apply') as apply, \
             patch.object(deploy, 'command', side_effect=[NEW['backend'], NEW['frontend']]):
            self.app.run(self.args)
            apply.assert_not_called()
        self.assertEqual(self.app.read('previous'), OLD)

    def test_failure_restores_previous_and_still_reports_failure(self):
        with self.assertRaisesRegex(deploy.DeployError, 'anterior restaurada'):
            self.run_update([deploy.DeployError('health failure'), None])
        self.assertEqual(self.app.read('current'), OLD)
        self.assertFalse((self.app.directory / 'BLOCKED').exists())
        self.assertIn('rolled-back', (self.app.directory / 'events.jsonl').read_text())

    def test_rollback_failure_blocks_following_operations(self):
        with self.assertRaisesRegex(deploy.DeployError, 'Deploy e rollback falharam'):
            self.run_update([deploy.DeployError('failure'), deploy.DeployError('rollback failure')])
        self.assertTrue((self.app.directory / 'BLOCKED').exists())
        self.assertEqual(json.loads((self.app.directory / 'pending.json').read_text())['from'], OLD)
        with self.assertRaisesRegex(deploy.DeployError, 'Recuperação pendente'):
            self.run_update()

    def test_changed_schema_requires_explicit_per_revision_approval(self):
        changed = {**NEW, 'migrations': '2' * 40}
        with self.assertRaisesRegex(deploy.DeployError, 'aprovação específica'):
            self.run_update(target=changed)
        self.assertEqual(self.app.read('current'), OLD)
        self.assertFalse((self.app.directory / 'pending.json').exists())

    def test_changed_schema_failure_never_restores_old_images(self):
        changed = {**NEW, 'migrations': '2' * 40}
        approval = self.app.directory / 'approved-migrations' / changed['revision']
        approval.parent.mkdir()
        approval.touch()
        with patch.object(self.app, 'apply', side_effect=deploy.DeployError('schema failure')) as apply, \
             patch.object(self.app, 'snapshot', return_value=OLD), \
             patch.object(self.app, 'health'), patch.object(self.app, 'config'), \
             patch.object(deploy, 'command', side_effect=lambda args: NEW['revision'] if any('org.opencontainers.image.revision' in a for a in args) else args[-1]):
            with self.assertRaisesRegex(deploy.DeployError, 'recuperação assistida'):
                self.app.run(argparse.Namespace(action='deploy', **changed))
            apply.assert_called_once_with(changed)
        self.assertFalse(approval.exists())
        self.assertTrue((self.app.directory / 'BLOCKED').exists())

    def test_pull_failure_does_not_apply_or_change_state(self):
        def command(args):
            if 'pull' in args:
                raise deploy.DeployError('pull failure')
            return args[-1]
        with patch.object(self.app, 'snapshot', return_value=OLD), \
             patch.object(self.app, 'health'), patch.object(self.app, 'config'), \
             patch.object(self.app, 'apply') as apply, patch.object(deploy, 'command', side_effect=command):
            with self.assertRaisesRegex(deploy.DeployError, 'pull failure'):
                self.app.run(self.args)
            apply.assert_not_called()
        self.assertEqual(self.app.read('current'), OLD)
        self.assertFalse((self.app.directory / 'BLOCKED').exists())

    def test_drift_prevents_update(self):
        with patch.object(self.app, 'snapshot', return_value={**OLD, 'backend': 'sha256:' + '9' * 64}), \
             patch.object(deploy, 'command', return_value=OLD['backend']), patch.object(self.app, 'apply') as apply:
            with self.assertRaisesRegex(deploy.DeployError, 'divergem'):
                self.app.run(self.args)
            apply.assert_not_called()

    def test_manual_rollback_requires_same_schema(self):
        deploy.atomic_json(self.app.directory / 'previous.json', {**OLD, 'migrations': '9' * 40})
        with self.assertRaisesRegex(deploy.DeployError, 'schema não é automático'):
            self.run_update(action='rollback')

    def test_manual_rollback_records_restored_version(self):
        deploy.atomic_json(self.app.directory / 'current.json', NEW)
        deploy.atomic_json(self.app.directory / 'previous.json', OLD)
        with patch.object(self.app, 'snapshot', return_value={**NEW, 'backend': OLD['backend'], 'frontend': OLD['frontend']}), \
             patch.object(self.app, 'health'), patch.object(self.app, 'config'), \
             patch.object(self.app, 'apply') as apply, \
             patch.object(deploy, 'command', side_effect=[OLD['backend'], OLD['frontend']]):
            self.args.action = 'rollback'
            self.app.run(self.args)
            apply.assert_called_once_with(OLD)
        self.assertEqual(self.app.read('current'), OLD)
        self.assertEqual(self.app.read('previous'), NEW)

    def test_bootstrap_captures_images_only_after_health_passes(self):
        (self.app.directory / 'current.json').unlink()
        args = argparse.Namespace(action='bootstrap', **OLD)
        with patch.object(self.app, 'snapshot', return_value=OLD), \
             patch.object(self.app, 'health') as health, patch.object(self.app, 'apply') as apply:
            self.app.run(args)
            health.assert_called_once_with(OLD)
            apply.assert_not_called()
        self.assertEqual(self.app.read('current'), OLD)

    def test_local_lock_prevents_concurrent_operation(self):
        previous_umask = os.umask(0o077)
        try:
            with (self.app.directory / 'lock').open('a') as held:
                deploy.fcntl.flock(held, deploy.fcntl.LOCK_EX | deploy.fcntl.LOCK_NB)
                with patch.object(deploy.sys, 'argv', ['deploy', 'status', self.tmp.name]), \
                     patch.object(deploy.Deployment, 'run') as run, patch('sys.stderr'):
                    self.assertEqual(deploy.main(), 1)
                    run.assert_not_called()
        finally:
            os.umask(previous_umask)

    def test_bootstrap_refuses_to_overwrite_existing_state(self):
        self.args.action = 'bootstrap'
        with self.assertRaisesRegex(deploy.DeployError, 'já realizado'):
            self.app.run(self.args)

    def test_apply_only_updates_application_without_dependencies(self):
        with patch.object(self.app, 'config'), patch.object(self.app, 'health'), patch.object(deploy, 'command') as command:
            self.app.apply(NEW, pull=True)
        commands = [call.args[0] for call in command.call_args_list]
        self.assertEqual(commands[0][-3:], ['pull', 'backend', 'frontend'])
        self.assertEqual(commands[1][-2:], ['backend', 'frontend'])
        self.assertIn('--no-deps', commands[1])
        self.assertIn('--wait', commands[1])
        self.assertIn('never', commands[1])

    def test_https_failure_is_not_accepted(self):
        opener = unittest.mock.Mock()
        opener.open.side_effect = OSError('TLS failure')
        self.app.origin = 'https://example.com'
        # Container checks pass; external certificate failure must fail deployment.
        def command(args):
            if '{{.State.Running}} {{.State.Health.Status}}' in args:
                return 'true healthy'
            return OLD['backend']
        with patch.object(self.app, 'config'), patch.object(deploy, 'command', side_effect=command), \
             patch.object(deploy.urllib.request, 'build_opener', return_value=opener), patch.object(deploy.time, 'sleep'):
            with self.assertRaisesRegex(deploy.DeployError, 'HTTPS falhou'):
                self.app.health(OLD)
        self.assertEqual(opener.open.call_count, 6)

    def test_command_suppresses_raw_errors_and_secrets(self):
        with patch.object(deploy.subprocess, 'run', side_effect=subprocess.CalledProcessError(1, ['docker'], stderr='DB_PASSWORD=secret')):
            with self.assertRaises(deploy.DeployError) as error:
                deploy.command(['docker', 'compose'])
        self.assertNotIn('secret', str(error.exception))

    def test_tags_and_missing_images_are_rejected(self):
        for bad in ('ghcr.io/owner/backend:latest', 'repo:sha', None):
            with self.assertRaises(deploy.DeployError):
                deploy.validate_state({**NEW, 'backend': bad})


class SshTest(unittest.TestCase):
    def setUp(self):
        spec = importlib.util.spec_from_file_location('transport', Path(__file__).with_name('deploy-ssh.py'))
        self.transport = importlib.util.module_from_spec(spec)
        spec.loader.exec_module(self.transport)
        env = {'DEPLOY_HOST': 'example.com', 'DEPLOY_USER': 'deploy', 'DEPLOY_PORT': '22',
               'DEPLOY_PATH': '/srv/finance', 'DEPLOY_SSH_KEY': 'synthetic-private-key',
               'DEPLOY_KNOWN_HOSTS': 'synthetic-host-key', 'RELEASE_SHA': NEW['revision'],
               'MIGRATIONS_SHA': NEW['migrations'], 'BACKEND_IMAGE': NEW['backend'],
               'FRONTEND_IMAGE': NEW['frontend']}
        self.env = patch.dict(os.environ, env)
        self.env.start()
        self.addCleanup(self.env.stop)

    def test_strict_authentication_and_temporary_keys(self):
        paths = []
        def run(args, **kwargs):
            self.assertIn('StrictHostKeyChecking=yes', args)
            self.assertIn('PasswordAuthentication=no', args)
            self.assertIn('KbdInteractiveAuthentication=no', args)
            self.assertIn('BatchMode=yes', args)
            self.assertIn('IdentitiesOnly=yes', args)
            self.assertNotIn('synthetic-private-key', ' '.join(args))
            self.assertNotIn('synthetic-host-key', ' '.join(args))
            self.assertIn(NEW['backend'], args[-1])
            key = Path(args[args.index('-i') + 1])
            self.assertEqual(key.stat().st_mode & 0o777, 0o600)
            self.assertEqual(key.read_text().strip(), 'synthetic-private-key')
            self.assertEqual(kwargs['stdin'], subprocess.DEVNULL)
            paths.append(key)
        previous_umask = os.umask(0o077)
        try:
            with patch.object(self.transport.subprocess, 'run', side_effect=run):
                self.transport.main()
        finally:
            os.umask(previous_umask)
        self.assertFalse(paths[0].exists())

    def test_shell_injection_and_invalid_inputs_never_connect(self):
        for key, value in [('DEPLOY_HOST', '-oProxyCommand=bad'), ('DEPLOY_PATH', '/srv/app;bad'),
                           ('DEPLOY_USER', 'deploy bad'), ('DEPLOY_PORT', '65536'),
                           ('BACKEND_IMAGE', 'repo:latest'), ('DEPLOY_KNOWN_HOSTS', '')]:
            with self.subTest(key=key), patch.dict(os.environ, {key: value}), \
                 patch.object(self.transport.subprocess, 'run') as run:
                with self.assertRaises(SystemExit):
                    self.transport.main()
                run.assert_not_called()


if __name__ == '__main__':
    unittest.main()
