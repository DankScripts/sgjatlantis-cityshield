#!/usr/bin/env python3
"""Tiny local read-only mirror used only to build inside the restricted workspace."""

from http.server import BaseHTTPRequestHandler, ThreadingHTTPServer
from pathlib import Path
from urllib.parse import urlsplit
import hashlib
import os
import subprocess
import ssl
import sys

UPSTREAMS = {
    "forge": "https://maven.minecraftforge.net/",
    "central": "https://repo.maven.apache.org/maven2/",
    "libraries": "https://libraries.minecraft.net/",
    "library": "https://libraries.minecraft.net/",
    "library0": "https://libraries.minecraft.net/",
    "mojang": "https://maven.mojang.com/",
    "plugins": "https://plugins.gradle.org/m2/",
    "piston0": "https://piston-meta.mojang.com/",
    "piston": "https://piston-meta.mojang.com/",
    "piston-data": "https://piston-data.mojang.com/",
}
CACHE = Path("/tmp/atlantis-maven-mirror-https-v2")
CACHE.mkdir(parents=True, exist_ok=True)


class Handler(BaseHTTPRequestHandler):
    protocol_version = "HTTP/1.1"

    def do_HEAD(self):
        self._serve(False)

    def do_GET(self):
        self._serve(True)

    def _serve(self, include_body):
        parts = urlsplit(self.path).path.lstrip("/").split("/", 1)
        if len(parts) != 2 or parts[0] not in UPSTREAMS or ".." in parts[1]:
            self.send_error(404)
            return
        url = UPSTREAMS[parts[0]] + parts[1]
        suffix = Path(parts[1]).suffix or ".bin"
        target = CACHE / (hashlib.sha256(url.encode()).hexdigest() + suffix)
        if not target.exists():
            temporary = target.with_suffix(target.suffix + ".part")
            result = subprocess.run([
                "curl", "-L", "--fail", "--silent", "--show-error",
                "--retry", "2", "--max-time", "180", "-A", "Gradle/8.8",
                "-o", str(temporary), url
            ], stdout=subprocess.DEVNULL, stderr=subprocess.DEVNULL)
            if result.returncode != 0:
                temporary.unlink(missing_ok=True)
                self.send_error(404)
                return
            if target.suffix == ".json":
                content = temporary.read_bytes()
                replacements = {
                    b"https://piston-meta.mojang.com/": b"https://127.0.0.1:48123/piston/",
                    b"https://piston-data.mojang.com/": b"https://127.0.0.1:48123/piston-data/",
                    b"https://libraries.minecraft.net/": b"https://127.0.0.1:48123/library/",
                }
                for old, new in replacements.items():
                    content = content.replace(old, new)
                temporary.write_bytes(content)
            os.replace(temporary, target)
        size = target.stat().st_size
        self.send_response(200)
        self.send_header("Content-Length", str(size))
        self.send_header("Content-Type", "application/octet-stream")
        self.end_headers()
        if include_body:
            with target.open("rb") as source:
                while chunk := source.read(1024 * 1024):
                    self.wfile.write(chunk)

    def log_message(self, format_string, *args):
        pass


if __name__ == "__main__":
    certificate, private_key = sys.argv[1:3]
    server = ThreadingHTTPServer(("127.0.0.1", 48123), Handler)
    context = ssl.SSLContext(ssl.PROTOCOL_TLS_SERVER)
    context.load_cert_chain(certificate, private_key)
    server.socket = context.wrap_socket(server.socket, server_side=True)
    server.serve_forever()
