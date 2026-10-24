#!/usr/bin/env python3
"""Index packaged Settings activities and preference labels, without opening pages."""
import argparse
import concurrent.futures
import hashlib
import json
import pathlib
import re
import subprocess
import zipfile


def tree(text):
    root = {"tag": "root", "attrs": {}, "children": []}
    stack = [(-1, root)]
    for line in text.splitlines():
        indent = len(line) - len(line.lstrip())
        element = re.match(r"\s*E: ([^ ]+)", line)
        if element:
            while stack[-1][0] >= indent:
                stack.pop()
            node = {"tag": element[1], "attrs": {}, "children": []}
            stack[-1][1]["children"].append(node)
            stack.append((indent, node))
        attr = re.match(r'\s*A: (?:http[^ ]*/apk/res/[^:]+:)?([^ (=]+)(?:\([^)]*\))?=(.*)', line)
        if attr:
            raw = attr[2].split(' (Raw:', 1)[0]
            if raw.startswith('"'):
                try:
                    raw = json.loads(raw)
                except ValueError:
                    raw = raw.strip('"')
            stack[-1][1]["attrs"][attr[1]] = raw
    return root


def walk(node):
    yield node
    for child in node["children"]:
        yield from walk(child)


def resources(text):
    result, current = {}, None
    for line in text.splitlines():
        m = re.match(r'\s*resource (0x[0-9a-f]+) (\S+)', line)
        if m:
            current = {"name": m[2], "values": {}}
            result[m[1]] = current
        value = re.match(r'\s*\(([^)]*)\) (".*")$', line)
        if current is not None and value:
            try:
                current["values"][value[1]] = json.loads(value[2])
            except ValueError:
                pass
    return result


def extract(apk, aapt):
    def dump(*args):
        return subprocess.check_output([aapt, 'dump', *args, str(apk)], text=True)
    res = resources(dump('resources'))
    def label(value):
        if not value or not value.startswith('@'):
            return value or ''
        entry = res.get(value[1:], {})
        strings = entry.get('values', {})
        return strings.get('zh-rCN', strings.get('', entry.get('name', value)))
    manifest = tree(dump('xmltree', '--file', 'AndroidManifest.xml'))
    manifest_node = next(n for n in walk(manifest) if n['tag'] == 'manifest')
    package = manifest_node['attrs']['package']
    activities = []
    for n in walk(manifest):
        if n['tag'] not in ['activity', 'activity-alias']:
            continue
        a = n['attrs']; name = a['name']
        if name.startswith('.'):
            name = package + name
        elif '.' not in name:
            name = package + '.' + name
        metadata = {c['attrs'].get('name'): c['attrs'].get('value') for c in n['children'] if c['tag'] == 'meta-data'}
        activities.append({
            'id': package + '/' + name, 'kind': 'activity', 'package': package,
            'class': name, 'title': label(a.get('label')), 'exported': a.get('exported'),
            'manifest_enabled': a.get('enabled', 'true'), 'permission': a.get('permission'),
            'fragment': metadata.get('com.android.settings.FRAGMENT_CLASS'),
            'actions': sorted({c['attrs']['name'] for c in walk(n) if c['tag'] == 'action'}),
            'verification': 'static_candidate',
        })
    with zipfile.ZipFile(apk) as z:
        paths = [p for p in z.namelist() if p.startswith('res/xml/') and p.endswith('.xml')]
    def preferences(path):
        nodes = tree(dump('xmltree', '--file', path))
        return [{'kind': 'preference', 'package': package, 'xml': path,
                 'widget': n['tag'], 'title': label(n['attrs'].get('title')),
                 'key': n['attrs'].get('key'), 'fragment': n['attrs'].get('fragment'),
                 'verification': 'static_candidate'}
                for n in walk(nodes) if n['attrs'].get('title')]
    prefs = []
    with concurrent.futures.ThreadPoolExecutor(max_workers=6) as pool:
        for rows in pool.map(preferences, paths):
            prefs.extend(rows)
    source = {'package': package, 'apk_sha256': hashlib.sha256(apk.read_bytes()).hexdigest(),
              'version': manifest_node['attrs'].get('versionName'), 'xml_files_scanned': len(paths)}
    return source, activities, prefs


def main():
    p = argparse.ArgumentParser()
    p.add_argument('--aapt2', required=True)
    p.add_argument('--apk', action='append', required=True, type=pathlib.Path)
    p.add_argument('--output', required=True, type=pathlib.Path)
    args = p.parse_args()
    output = {'schema': 1, 'sources': [], 'activities': [], 'preferences': [],
              'coverage': 'Packaged manifest and res/xml labels only. Dynamic pages, overlays and runtime availability are not proven.'}
    for apk in args.apk:
        source, activities, prefs = extract(apk, args.aapt2)
        output['sources'].append(source); output['activities'].extend(activities); output['preferences'].extend(prefs)
    args.output.write_text(json.dumps(output, ensure_ascii=False, indent=2) + '\n')
    print(json.dumps({'activities': len(output['activities']), 'preferences': len(output['preferences']), 'sources': output['sources']}, ensure_ascii=False))


if __name__ == '__main__':
    main()
