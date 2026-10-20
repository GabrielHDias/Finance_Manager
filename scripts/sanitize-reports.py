#!/usr/bin/env python3
"""Export only allowlisted test metadata; never publish raw output/error contexts."""
import argparse
import json
from pathlib import Path
import xml.etree.ElementTree as ET


def backend(folder):
    results = []
    for path in sorted(folder.glob('TEST-*.xml')):
        root = ET.parse(path).getroot()
        for test in root.findall('testcase'):
            status = 'failed' if test.find('failure') is not None or test.find('error') is not None else 'skipped' if test.find('skipped') is not None else 'passed'
            results.append({'suite': test.get('classname'), 'test': test.get('name'), 'status': status,
                            'duration_seconds': float(test.get('time', '0'))})
    return results


def browser(path):
    results = []
    def visit(suites):
        for suite in suites:
            visit(suite.get('suites', []))
            for spec in suite.get('specs', []):
                for test in spec.get('tests', []):
                    for result in test.get('results', []):
                        results.append({'test': spec['title'], 'status': result['status'],
                                        'duration_ms': result.get('duration', 0)})
    visit(json.loads(path.read_text()).get('suites', []))
    return results


if __name__ == '__main__':
    parser = argparse.ArgumentParser(description=__doc__)
    source = parser.add_mutually_exclusive_group(required=True)
    source.add_argument('--backend', type=Path)
    source.add_argument('--browser', type=Path)
    parser.add_argument('--revision', required=True)
    parser.add_argument('--output', type=Path, required=True)
    args = parser.parse_args()
    results = backend(args.backend) if args.backend else browser(args.browser)
    if not results:
        parser.exit(1, 'Nenhum resultado disponível para sanitização\n')
    args.output.parent.mkdir(parents=True, exist_ok=True)
    args.output.write_text(json.dumps({'revision': args.revision, 'results': results}, indent=2) + '\n')
