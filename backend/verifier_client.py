"""Signed client for the Tremend Verifier website API (Starter Pack pages 8 and 11)."""
from __future__ import annotations

import hashlib
import hmac
import json
import time
import urllib.error
import urllib.request
import uuid
from typing import Optional
from urllib.parse import urlsplit


class VerifierError(Exception):
    def __init__(self, status: int, code: str):
        super().__init__(f"verifier HTTP {status} {code}")
        self.status = status
        self.code = code


def signed_headers(key: str, secret: str, method: str, path: str, body: bytes = b"",
                   timestamp: Optional[str] = None, nonce: Optional[str] = None) -> dict:
    timestamp = timestamp or str(int(time.time()))
    nonce = nonce or str(uuid.uuid4())
    canonical = "\n".join([method.upper(), path, timestamp, nonce, hashlib.sha256(body).hexdigest()])
    signature = hmac.new(secret.encode(), canonical.encode(), hashlib.sha256).hexdigest()
    return {
        "X-Verifier-Key": key,
        "X-Verifier-Timestamp": timestamp,
        "X-Verifier-Nonce": nonce,
        "X-Verifier-Signature": signature,
    }


class VerifierClient:
    def __init__(self, base_url: str, key: str, secret: str, timeout: float = 10.0):
        self._base_url = base_url.rstrip("/")
        self._key = key
        self._secret = secret
        self._timeout = timeout

    def start(self, profile: str, return_uri: Optional[str] = None) -> dict:
        body = {"profile": profile}
        if return_uri:
            body["returnUri"] = return_uri
        return self._json("POST", "/api/website/verifications", body)

    def status(self, transaction_id: str) -> dict:
        return self._json("GET", f"/api/website/verifications/{transaction_id}")

    def claim(self, transaction_id: str, response_code: str) -> dict:
        return self._json("POST", f"/api/website/verifications/{transaction_id}/claim",
                          {"responseCode": response_code})

    def collect(self, transaction_id: str) -> str:
        """Returns the sealed result (compact JWE)."""
        raw = self._send("GET", f"/api/website/verifications/{transaction_id}/result",
                         accept="application/jose")
        return raw.decode().strip()

    def acknowledge(self, transaction_id: str) -> None:
        self._send("DELETE", f"/api/website/verifications/{transaction_id}/result")

    def jwks(self) -> dict:
        return json.loads(self._send("GET", "/.well-known/jwks.json", sign=False))

    def _json(self, method: str, path: str, body: Optional[dict] = None) -> dict:
        raw = self._send(method, path, body=body)
        return json.loads(raw) if raw else {}

    def _send(self, method: str, path: str, body: Optional[dict] = None,
              accept: str = "application/json", sign: bool = True) -> bytes:
        data = json.dumps(body, separators=(",", ":")).encode() if body is not None else b""
        headers = {"Accept": accept}
        if body is not None:
            headers["Content-Type"] = "application/json"
        if sign:
            # The signature covers the path without host or query and the exact body bytes sent.
            headers.update(signed_headers(self._key, self._secret, method, urlsplit(path).path, data))
        request = urllib.request.Request(self._base_url + path, data=data or None,
                                         headers=headers, method=method)
        try:
            with urllib.request.urlopen(request, timeout=self._timeout) as response:
                return response.read()
        except urllib.error.HTTPError as error:
            code = ""
            try:
                code = json.loads(error.read() or b"{}").get("code", "")
            except ValueError:
                pass
            raise VerifierError(error.code, code) from None
