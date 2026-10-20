import argparse
import importlib.util
import io
import json
import os
from pathlib import Path
import subprocess
import tempfile
import unittest
from unittest.mock import patch, Mock


def load(name, file):
    spec = importlib.util.spec_from_file_location(name, Path(__file__).with_name(file))
    module = importlib.util.module_from_spec(spec)
    spec.loader.exec_module(module)
    return module


deploy = load('runtime', 'deploy-ec2.py')
gateway = load('gateway', 'deploy-gateway.py')
transport = load('transport', 'deploy-ssh.py')
STATE = {'revision': 'a'*40, 'migrations': 'b'*40, 'backend': 'sha256:'+'c'*64, 'frontend': 'sha256:'+'d'*64}
CONFIG = {'root': '/srv/finance', 'registry': 'ghcr.io/owner/repo'}


class RuntimeTest(unittest.TestCase):
    def test_health_override_tracks_version_without_modifying_original(self):
        with tempfile.TemporaryDirectory() as directory, patch.dict(os.environ):
            root = Path(directory)
            original = root / 'compose.demo.yml'
            original.write_text('original compose')
            app = deploy.Deployment(root)
            app.compose(STATE)
            override = app.directory / 'healthcheck.override.json'
            self.assertIn('/health/ready', json.loads(override.read_text())['services']['backend']['healthcheck']['test'][1])
            app.compose({**STATE, 'health_profile': 'legacy-accounts-v1'})
            self.assertIn('/accounts', override.read_text())
            self.assertEqual(original.read_text(), 'original compose')
            self.assertEqual(app.base.count('-f'), 2)

    def test_legacy_health_requires_real_jdbc_connection_and_expected_http_statuses(self):
        with tempfile.TemporaryDirectory() as directory:
            app = deploy.Deployment(directory)
            app.origin = 'https://example.com'
            def command(args, **kwargs):
                if '{{.State.Running}} {{.State.Health.Status}}' in args:
                    return 'true healthy'
                if 'psql' in args:
                    return 't'
                return 'fixture-image'
            response = Mock(status=200)
            response.__enter__ = Mock(return_value=response)
            response.__exit__ = Mock(return_value=False)
            opener = Mock()
            opener.open.side_effect = [response, deploy.urllib.error.HTTPError('https://example.com/api/accounts',401,'Unauthorized',{},None)]
            with patch.object(app, 'config'), patch.object(deploy, 'command', side_effect=command), \
                 patch.object(deploy.urllib.request, 'build_opener', return_value=opener):
                app.health({**STATE, 'health_profile': 'legacy-accounts-v1'})
            self.assertEqual(opener.open.call_count, 2)
            with patch.object(app, 'config'), patch.object(deploy, 'command', side_effect=lambda args,**kw: 'true healthy' if '{{.State.Running}} {{.State.Health.Status}}' in args else ('f' if 'psql' in args else 'fixture')):
                with self.assertRaisesRegex(deploy.DeployError, 'conexão JDBC'):
                    app.health({**STATE, 'health_profile':'legacy-accounts-v1'})

    def test_registry_uses_stdin_and_removes_temporary_login(self):
        token = 'ghs_' + 'x'*40
        original = os.environ.get('DOCKER_CONFIG')
        paths = []
        with patch.object(deploy.sys,'stdin',io.StringIO(token+'\n')), patch.object(deploy.subprocess,'run') as run:
            with deploy.registry_login('operator'):
                paths.append(Path(os.environ['DOCKER_CONFIG']))
                self.assertTrue(paths[0].exists())
                self.assertEqual(run.call_args.kwargs['input'],token+'\n')
                self.assertNotIn(token,' '.join(run.call_args.args[0]))
        self.assertFalse(paths[0].exists())
        self.assertEqual(os.environ.get('DOCKER_CONFIG'),original)

    def test_registry_accepts_opaque_tokens_with_punctuation_without_shell_evaluation(self):
        token='opaque.token-with+punctuation/=_'+'x'*40
        with patch.object(deploy.sys,'stdin',io.StringIO(token+'\n')), patch.object(deploy.subprocess,'run') as run:
            with deploy.registry_login('operator'):
                self.assertEqual(run.call_args.kwargs['input'],token+'\n')
                self.assertNotIn(token,' '.join(run.call_args.args[0]))
                self.assertFalse(run.call_args.kwargs.get('shell',False))

    def test_failed_registry_login_still_removes_private_directory(self):
        paths=[]
        def fail(*args,**kwargs):
            paths.append(Path(os.environ['DOCKER_CONFIG']))
            raise subprocess.CalledProcessError(1,args[0],stderr='do not log token')
        with patch.object(deploy.sys,'stdin',io.StringIO('ghs_'+'x'*40)), patch.object(deploy.subprocess,'run',side_effect=fail):
            with self.assertRaisesRegex(deploy.DeployError,'Login GHCR'):
                with deploy.registry_login('operator'):
                    self.fail('login failed')
        self.assertFalse(paths[0].exists())

    def request(self):
        return ['python3','/srv/finance/deploy-cd.py','deploy','/srv/finance',
                '--revision','a'*40,'--migrations','b'*40,
                '--backend','ghcr.io/owner/repo-backend@sha256:'+'c'*64,
                '--frontend','ghcr.io/owner/repo-frontend@sha256:'+'d'*64]

    def test_gateway_only_allows_repository_images_and_fixed_entrypoint(self):
        import shlex
        command=self.request()
        self.assertEqual(gateway.validate_request(shlex.join(command),CONFIG),command)
        for position,value in ((1,'/tmp/arbitrary.py'),(2,'bootstrap'),(3,'/other/path'),(9,'ghcr.io/attacker/backend@sha256:'+'c'*64)):
            changed=command.copy();changed[position]=value
            with self.assertRaises(ValueError):
                gateway.validate_request(shlex.join(changed),CONFIG)
        with self.assertRaises(ValueError):
            gateway.validate_request(shlex.join(command+['--legacy-health','yes']),CONFIG)
        with self.assertRaises(ValueError):
            gateway.validate_request(shlex.join(command+['--revision','f'*40]),CONFIG)
        self.assertEqual(gateway.validate_request('python3 /srv/finance/deploy-cd.py status /srv/finance',CONFIG)[2],'status')

    def test_transport_sends_registry_token_only_on_stdin(self):
        token='ghs_'+'x'*40
        environment={'DEPLOY_HOST':'example.com','DEPLOY_USER':'deploy','DEPLOY_PORT':'22',
                     'DEPLOY_PATH':'/srv/finance','DEPLOY_SSH_KEY':'fixture','DEPLOY_KNOWN_HOSTS':'fixture',
                     'RELEASE_SHA':'a'*40,'MIGRATIONS_SHA':'b'*40,
                     'BACKEND_IMAGE':'ghcr.io/owner/repo-backend@sha256:'+'c'*64,
                     'FRONTEND_IMAGE':'ghcr.io/owner/repo-frontend@sha256:'+'d'*64,
                     'DEPLOY_REGISTRY_USER':'operator','DEPLOY_REGISTRY_TOKEN':token}
        previous=os.umask(0o077)
        try:
            with patch.dict(os.environ,environment,clear=True), patch.object(transport.subprocess,'run') as run:
                transport.main()
                self.assertEqual(run.call_args.kwargs['input'],token+'\n')
                self.assertNotIn(token,' '.join(run.call_args.args[0]))
                self.assertIn('--registry-user operator',run.call_args.args[0][-1])
        finally:
            os.umask(previous)

    def test_transport_status_does_not_require_images_or_send_registry_token(self):
        environment={'DEPLOY_HOST':'example.com','DEPLOY_USER':'deploy','DEPLOY_PORT':'22',
                     'DEPLOY_PATH':'/srv/finance','DEPLOY_SSH_KEY':'fixture','DEPLOY_KNOWN_HOSTS':'fixture',
                     'DEPLOY_ACTION':'status'}
        previous=os.umask(0o077)
        try:
            with patch.dict(os.environ,environment,clear=True), patch.object(transport.subprocess,'run') as run:
                transport.main()
                self.assertEqual(run.call_args.args[0][-1],'python3 /srv/finance/deploy-cd.py status /srv/finance')
                self.assertEqual(run.call_args.kwargs['stdin'],subprocess.DEVNULL)
        finally:
            os.umask(previous)


if __name__=='__main__':
    unittest.main()
