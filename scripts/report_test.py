import importlib.util
import json
from pathlib import Path
import tempfile
import unittest
spec = importlib.util.spec_from_file_location('reports', Path(__file__).with_name('sanitize-reports.py'))
reports = importlib.util.module_from_spec(spec); spec.loader.exec_module(reports)
class ReportTest(unittest.TestCase):
    def test_backend_discards_logs_properties_and_error_messages(self):
        with tempfile.TemporaryDirectory() as folder:
            root = Path(folder)
            (root/'TEST-example.xml').write_text('<testsuite><properties><property name="secret" value="private"/></properties><testcase classname="Suite" name="test" time="1"><failure>Bearer secret</failure><system-out>private data</system-out></testcase></testsuite>')
            result = json.dumps(reports.backend(root))
            self.assertIn('failed',result)
            for value in ['private','Bearer','secret','system-out']: self.assertNotIn(value,result)
    def test_browser_discards_errors_attachments_and_runtime_config(self):
        with tempfile.TemporaryDirectory() as folder:
            path = Path(folder)/'results.json'
            path.write_text(json.dumps({'config':{'secret':'private'},'suites':[{'specs':[{'title':'test','tests':[{'results':[{'status':'failed','duration':1,'error':{'message':'Bearer secret'},'attachments':[{'body':'private'}]}]}]}]}]}))
            result = json.dumps(reports.browser(path))
            for value in ['Bearer','private','secret','attachments','error']: self.assertNotIn(value,result)
if __name__ == '__main__': unittest.main()
