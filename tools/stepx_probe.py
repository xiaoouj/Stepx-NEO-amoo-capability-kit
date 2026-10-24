#!/usr/bin/env python3
"""Read-only StepX REST/WebSocket probe; never sends chat.send.

Accepts a raw Cookie file or stepxmini shared_prefs/auth.xml. Output contains
status codes, model names and event shapes, not credentials or chat contents.
"""
import argparse
import base64
import collections
import hashlib
import json
import os
from pathlib import Path
import socket
import ssl
import struct
import time
import urllib.error
import urllib.parse
import urllib.request
import xml.etree.ElementTree as ET


class WebSocket:
    def __init__(self, cookie):
        self.sock = ssl.create_default_context().wrap_socket(
            socket.create_connection(("studio-api.stepx.com", 443), timeout=10),
            server_hostname="studio-api.stepx.com",
        )
        self.sock.settimeout(2)
        self.buffer = b""
        key = base64.b64encode(os.urandom(16)).decode()
        self.sock.sendall((
            "GET /ws HTTP/1.1\r\nHost: studio-api.stepx.com\r\n"
            "Upgrade: websocket\r\nConnection: Upgrade\r\n"
            f"Sec-WebSocket-Key: {key}\r\nSec-WebSocket-Version: 13\r\n"
            f"Origin: https://studio.stepx.com\r\nCookie: {cookie}\r\n\r\n"
        ).encode())
        while b"\r\n\r\n" not in self.buffer:
            part = self.sock.recv(8192)
            if not part:
                raise EOFError("Handshake connection closed")
            self.buffer += part
            if len(self.buffer) > 65536:
                raise ValueError("Oversized handshake")
        head, self.buffer = self.buffer.split(b"\r\n\r\n", 1)
        self.status = int(head.split(b"\r\n", 1)[0].split()[1])
        headers = dict(line.split(b":", 1) for line in head.split(b"\r\n")[1:] if b":" in line)
        headers = {k.lower(): v.strip() for k, v in headers.items()}
        expected = base64.b64encode(hashlib.sha1((key + "258EAFA5-E914-47DA-95CA-C5AB0DC85B11").encode()).digest())
        if self.status != 101 or headers.get(b"sec-websocket-accept") != expected:
            self.sock.close()
            raise ValueError(f"Invalid WebSocket handshake (HTTP {self.status})")

    def send(self, payload, opcode=1):
        data = json.dumps(payload).encode() if opcode == 1 else payload
        n = len(data)
        header = bytes([128 | opcode, 128 | n]) if n < 126 else bytes([128 | opcode, 254]) + struct.pack("!H", n)
        mask = os.urandom(4)
        self.sock.sendall(header + mask + bytes(b ^ mask[i % 4] for i, b in enumerate(data)))

    def read(self, n):
        while len(self.buffer) < n:
            part = self.sock.recv(8192)
            if not part:
                raise EOFError("WebSocket closed")
            self.buffer += part
        value, self.buffer = self.buffer[:n], self.buffer[n:]
        return value

    def receive(self):
        a, b = self.read(2)
        size = b & 127
        if size == 126:
            size = struct.unpack("!H", self.read(2))[0]
        elif size == 127:
            size = struct.unpack("!Q", self.read(8))[0]
        if size > 8 * 1024 * 1024:
            raise ValueError("Oversized WebSocket frame")
        mask = self.read(4) if b & 128 else None
        raw = self.read(size)
        if mask:
            raw = bytes(x ^ mask[i % 4] for i, x in enumerate(raw))
        if not a & 128:
            raise ValueError("Fragmented frames are unsupported by this probe")
        opcode = a & 15
        if opcode == 8:
            raise EOFError("WebSocket close frame")
        if opcode == 9:
            self.send(raw, opcode=10)
        return json.loads(raw) if opcode == 1 else None


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--cookie-file", type=Path, required=True)
    parser.add_argument("--project", help="Optional owned project ID for read-only replay subscription")
    parser.add_argument("--output", type=Path)
    args = parser.parse_args()
    cookie = args.cookie_file.read_text().strip()
    if cookie.startswith("<"):
        root = ET.fromstring(cookie)
        cookie = next(e.text for e in root if e.get("name") == "cookie")
    if not cookie or "\r" in cookie or "\n" in cookie:
        raise ValueError("Expected a single-line Cookie header")
    result = {"probe": "read-only; no generation request", "rest": {}, "websocket": {}}
    for path in ["/api/identity/me", "/api/chat-models", "/api/builder-models"]:
        req = urllib.request.Request("https://studio.stepx.com" + path, headers={"Cookie": cookie})
        try:
            with urllib.request.urlopen(req, timeout=15) as response:
                value = json.load(response)
                result["rest"][path] = {"status": response.status}
                if path.endswith("models"):
                    result["rest"][path]["models"] = [
                        {"id": item.get("id"), "name": item.get("name")}
                        for item in value.get("items", [])
                    ]
        except urllib.error.HTTPError as error:
            result["rest"][path] = {"status": error.code}
    ws = WebSocket(cookie)
    counts, shapes = collections.Counter(), {}
    try:
        ws.send({"type": "ping"})
        if args.project:
            ws.send({"type": "project.switch", "projectId": args.project,
                     "payload": {"projectSwitch": {"resumeSeq": {}}}})
        deadline = time.monotonic() + 10
        while time.monotonic() < deadline and sum(counts.values()) < 256:
            try:
                event = ws.receive()
            except (socket.timeout, EOFError):
                break
            if event:
                kind = event.get("type", "unknown")
                counts[kind] += 1
                shapes[kind] = {"keys": sorted(event), "payloadKeys": sorted(event.get("payload") or {})}
                if not args.project and kind == "pong":
                    break
    finally:
        ws.sock.close()
    result["websocket"] = {"handshake": ws.status, "eventCounts": dict(counts), "eventShapes": shapes,
                           "coverage": "bounded observation, maximum 10 seconds / 256 events; project replay may contain historical events"}
    output = json.dumps(result, ensure_ascii=False, indent=2) + "\n"
    if args.output:
        args.output.write_text(output)
    print(output, end="")


if __name__ == "__main__":
    main()
