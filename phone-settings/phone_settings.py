#!/usr/bin/env python3
"""Amoo-side local Settings client. Authentication stays in a private local file."""
import argparse
import json
import pathlib
import sys
import urllib.error
import urllib.request

BASE = pathlib.Path(__file__).resolve().parent
DATA = BASE if (BASE / 'routes.json').exists() else BASE / 'data'


def request(route, data=None):
    config = json.loads((BASE / '.broker.json').read_text())
    req = urllib.request.Request('http://127.0.0.1:' + str(config['port']) + route,
        headers={'Authorization': 'Bearer ' + config['token'], 'Content-Type': 'application/json'},
        data=None if data is None else json.dumps(data).encode())
    # Never send this local token through an environment-configured HTTP proxy.
    opener = urllib.request.build_opener(urllib.request.ProxyHandler({}))
    try:
        with opener.open(req, timeout=22) as response:
            return json.load(response)
    except urllib.error.HTTPError as error:
        return json.load(error)
    except OSError:
        return {'ok': False, 'error': '本机设置服务不可用；可能重启后临时 Root 或服务已退出。请重新连接并启动服务。'}


def main():
    p = argparse.ArgumentParser()
    sub = p.add_subparsers(dest='command', required=True)
    sub.add_parser('routes')
    search = sub.add_parser('search'); search.add_argument('query')
    op = sub.add_parser('open'); op.add_argument('id')
    for action in ['health', 'home', 'back', 'recents', 'shutdown']:
        sub.add_parser(action)
    args = p.parse_args()
    if args.command == 'routes':
        result = json.loads((DATA / 'routes.json').read_text())
    elif args.command == 'search':
        query = args.query.casefold()
        index = json.loads((DATA / 'settings-index.json').read_text())
        routes = json.loads((DATA / 'routes.json').read_text())['routes']
        matches = lambda row: query in json.dumps(row, ensure_ascii=False).casefold()
        result = {'routes': [r for r in routes if matches(r)],
            'activities': [r for r in index['activities'] if matches(r)],
            'preferences': [r for r in index['preferences'] if matches(r)],
            'note': '索引候选不等于可用直达入口；open 仅接受 routes 中的 id。'}
    elif args.command == 'open':
        result = request('/open', {'id': args.id})
    elif args.command in ['health', 'shutdown']:
        result = request('/' + args.command, None if args.command == 'health' else {})
    else:
        result = request('/navigate', {'action': args.command})
    print(json.dumps(result, ensure_ascii=False, indent=2))
    if result.get('ok') is False or 'error' in result:
        sys.exit(1)


if __name__ == '__main__':
    main()
