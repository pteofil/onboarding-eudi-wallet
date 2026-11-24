"""Runs the Lambda handler on http://0.0.0.0:8081 for the phone (adb reverse tcp:8081 tcp:8081).

Reads backend/.env if present. Without verifier settings it falls back to the stub verifier.
"""
from __future__ import annotations

import json
import os
from http.server import BaseHTTPRequestHandler, ThreadingHTTPServer
from pathlib import Path

PORT = int(os.environ.get("PORT", "8081"))


def load_env_file(path: Path) -> None:
    if not path.exists():
        return
    for line in path.read_text().splitlines():
        line = line.strip()
        if line and not line.startswith("#") and "=" in line:
            key, value = line.split("=", 1)
            os.environ.setdefault(key.strip(), value.strip())


class Handler(BaseHTTPRequestHandler):
    def _handle(self):
        from handler import lambda_handler

        length = int(self.headers.get("Content-Length") or 0)
        body = self.rfile.read(length).decode() if length else ""
        event = {"rawPath": self.path.split("?")[0], "body": body,
                 "requestContext": {"http": {"method": self.command}}}
        result = lambda_handler(event)
        payload = result["body"].encode()
        self.send_response(result["statusCode"])
        self.send_header("Content-Type", "application/json")
        self.send_header("Content-Length", str(len(payload)))
        self.end_headers()
        self.wfile.write(payload)

    do_GET = do_POST = _handle


if __name__ == "__main__":
    load_env_file(Path(__file__).with_name(".env"))
    if not os.environ.get("VERIFIER_WEBSITE_SECRET"):
        os.environ.setdefault("STUB_VERIFIER", "1")
    os.environ.setdefault("VERIFIER_RETURN_URI", "ro.banking40.team01://same-device")
    mode = "STUB verifier" if os.environ.get("STUB_VERIFIER") == "1" else "Tremend verifier"
    print(f"VerifiedStart backend on :{PORT} ({mode})")
    ThreadingHTTPServer(("0.0.0.0", PORT), Handler).serve_forever()
