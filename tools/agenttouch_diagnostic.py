#!/usr/bin/env python3
import os
"""ADB diagnostic client for the installed AgentTouch AppFunctions, not the AMOO agent."""
import argparse
import json
import pathlib
import shlex
import subprocess
import time
import uuid

ADB = os.environ.get('AMOO_ADB_PATH', os.path.expanduser('~/.workbuddy/binaries/android/platform-tools/adb'))
PACKAGE = 'life.suk.agenttouch'
PREFIX = PACKAGE + '.AgentFunctionService#'


def call(serial, function, params):
    command = ['cmd', 'app_function', 'execute-app-function', '--package', PACKAGE,
               '--function', PREFIX + function, '--parameters', json.dumps(params, ensure_ascii=False),
               '--timeout-duration', '25']
    result = subprocess.run([ADB, '-s', serial, 'shell', shlex.join(command)],
        text=True, capture_output=True, timeout=32)
    if result.returncode:
        return {'ok':False, 'layer':'android_appfunction', 'exitCode':result.returncode,
            'message':result.stdout.strip() or result.stderr.strip()}
    document = json.loads(result.stdout)
    value = document.get('androidAppfunctionsReturnValue', [document])[0]
    for _ in range(3):
        if not isinstance(value, str): break
        value = json.loads(value)
    return value


def main():
    p = argparse.ArgumentParser(description=__doc__)
    p.add_argument('--serial', required=True)
    p.add_argument('--output', type=pathlib.Path)
    sub = p.add_subparsers(dest='action', required=True)
    sub.add_parser('health')
    observe = sub.add_parser('observe'); observe.add_argument('package')
    execute = sub.add_parser('execute'); execute.add_argument('steps', type=pathlib.Path)
    execute.add_argument('--save-request', required=True, type=pathlib.Path)
    args = p.parse_args()
    if args.action == 'health':
        value = call(args.serial, 'health', {})
    elif args.action == 'observe':
        value = call(args.serial, 'observe', {'expectedPackage':args.package})
    else:
        # Never silently retry a mutation; the exact request can be retained for diagnosis.
        health = call(args.serial, 'health', {})
        if not health.get('serviceConnected') or health.get('paused') or not health.get('systemAgentEnabled'):
            value = {'ok':False, 'message':'服务未连接、已暂停或系统Agent授权关闭；未执行动作', 'health':health}
        else:
            steps = json.loads(args.steps.read_text())
            if not isinstance(steps, list) or not 1 <= len(steps) <= 20:
                raise ValueError('steps must be a JSON array containing 1-20 actions')
            request = {'protocolVersion':1, 'requestId':'codex-test-' + uuid.uuid4().hex,
                'deviceId':health['deviceId'], 'deadline':int(time.time()*1000)+45000, 'steps':steps}
            if args.save_request.exists():
                raise FileExistsError('Refusing to replace a retained request; choose a new path')
            args.save_request.write_text(json.dumps(request, ensure_ascii=False, indent=2)+'\n')
            value = call(args.serial, 'execute', {'requestJson':json.dumps(request, ensure_ascii=False)})
    encoded = json.dumps(value, ensure_ascii=False, indent=2)+'\n'
    if args.output: args.output.write_text(encoded)
    print(encoded, end='')
    if value.get('ok') is False or value.get('status') in ('failed', 'unknown'):
        raise SystemExit(1)


if __name__ == '__main__': main()
