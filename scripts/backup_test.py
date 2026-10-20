import importlib.util
import json
from pathlib import Path
import tempfile
import time
import unittest
from unittest.mock import patch
spec = importlib.util.spec_from_file_location('backup', Path(__file__).with_name('backup.py'))
backup = importlib.util.module_from_spec(spec)
spec.loader.exec_module(backup)

class BackupTest(unittest.TestCase):
    def test_integrity_age_and_empty(self):
        with tempfile.TemporaryDirectory() as folder:
            root = Path(folder); archive = root / 'backup.dump.age'; archive.write_bytes(b'ciphertext')
            metadata = root / 'backup.json'
            info = {'file': archive.name, 'sha256': backup.checksum(archive), 'created_at': time.time()}
            metadata.write_text(json.dumps(info))
            self.assertEqual(backup.verify(metadata,26), archive)
            info['created_at'] -= 27*3600; metadata.write_text(json.dumps(info))
            with self.assertRaises(ValueError): backup.verify(metadata,26)
            info['created_at'] = time.time(); metadata.write_text(json.dumps(info)); archive.write_bytes(b'tampered')
            with self.assertRaises(ValueError): backup.verify(metadata,26)
            archive.write_bytes(b'')
            with self.assertRaises(ValueError): backup.verify(metadata,26)

    def test_restore_refuses_operational_destinations_before_any_command(self):
        with patch.object(backup, 'run') as run:
            with self.assertRaises(ValueError): backup.restore(Path('unused'),Path('key'),'finance_manager',26)
            with patch.dict('os.environ',{'PGHOST':'production.example'}):
                with self.assertRaises(ValueError): backup.restore(Path('unused'),Path('key'),'fm_restore_test',26)
            run.assert_not_called()

    def test_backup_checks_dump_before_encryption_and_publishes_verified_metadata(self):
        with tempfile.TemporaryDirectory() as folder:
            def execute(args, **kwargs):
                if args[0] == 'pg_dump': kwargs['stdout'].write(b'test custom dump')
                if args[0] == 'age': Path(args[args.index('--output')+1]).write_bytes(b'encrypted fixture')
            with patch.object(backup,'run',side_effect=execute) as run:
                metadata = backup.backup(Path(folder),'test-recipient',30)
                self.assertTrue(backup.verify(metadata,26).exists())
                self.assertEqual([call.args[0][0] for call in run.call_args_list],['pg_dump','pg_restore','age'])

if __name__ == '__main__': unittest.main()
