"""Opens and checks a sealed Tremend Verifier result (Starter Pack page 11).

Mirrors examples/open-verifier-result.mjs. Claims are only usable after every
check here passes AND outcome.status == SATISFIED.
"""
from __future__ import annotations

import json
import time
from typing import Optional

from jwcrypto import jwe, jwk, jws

ALLOWED_KEY_ALGS = {"ECDH-ES+A256KW", "RSA-OAEP-256"}
ALLOWED_ENC = {"A256GCM"}
RESULT_TYP = "eudi-result+jwt"
RESULT_SCHEMA = "eudi-verification-result/v1"
CLOCK_TOLERANCE_S = 60


class ResultRejected(Exception):
    """The sealed result must not be used. `reason` is safe to log (no claim values)."""

    def __init__(self, reason: str):
        super().__init__(reason)
        self.reason = reason


def load_private_key(pem: bytes) -> jwk.JWK:
    return jwk.JWK.from_pem(pem)


def open_result(sealed: str, private_key: jwk.JWK, verifier_jwks: dict, issuer: str,
                audience: str, expected_transaction_id: str, now: Optional[float] = None) -> dict:
    now = time.time() if now is None else now
    signed = _decrypt(sealed, private_key)
    payload = _verify_signature(signed, verifier_jwks)
    _check_claims(payload, issuer, audience, expected_transaction_id, now)
    return payload


def _decrypt(sealed: str, private_key: jwk.JWK) -> str:
    try:
        token = jwe.JWE()
        token.deserialize(sealed)
        header = token.jose_header
    except Exception:
        raise ResultRejected("malformed_jwe") from None
    if header.get("alg") not in ALLOWED_KEY_ALGS or header.get("enc") not in ALLOWED_ENC:
        raise ResultRejected("jwe_algorithm_not_allowed")
    if header.get("kid") and private_key.get("kid") and header["kid"] != private_key["kid"]:
        raise ResultRejected("unknown_result_key")
    try:
        token.allowed_algs = sorted(ALLOWED_KEY_ALGS | ALLOWED_ENC)
        token.decrypt(private_key)
    except Exception:
        raise ResultRejected("decryption_failed") from None
    return token.payload.decode()


def _verify_signature(signed: str, verifier_jwks: dict) -> dict:
    try:
        token = jws.JWS()
        token.deserialize(signed)
        header = token.jose_header
    except Exception:
        raise ResultRejected("malformed_jws") from None
    if header.get("alg") != "ES256":
        raise ResultRejected("jws_algorithm_not_allowed")
    if header.get("typ") != RESULT_TYP:
        raise ResultRejected("unexpected_typ")
    keys = jwk.JWKSet.from_json(json.dumps(verifier_jwks))
    candidates = [keys.get_key(header["kid"])] if header.get("kid") else list(keys)
    for key in filter(None, candidates):
        try:
            token.allowed_algs = ["ES256"]
            token.verify(key)
            return json.loads(token.payload)
        except Exception:
            continue
    raise ResultRejected("bad_signature")


def _check_claims(payload: dict, issuer: str, audience: str, transaction_id: str, now: float) -> None:
    for name in ("iat", "exp", "jti"):
        if name not in payload:
            raise ResultRejected(f"missing_{name}")
    if payload.get("iss") != issuer:
        raise ResultRejected("wrong_issuer")
    aud = payload.get("aud")
    if audience not in (aud if isinstance(aud, list) else [aud]):
        raise ResultRejected("wrong_audience")
    if now > payload["exp"] + CLOCK_TOLERANCE_S:
        raise ResultRejected("expired")
    if payload["iat"] > now + CLOCK_TOLERANCE_S:
        raise ResultRejected("issued_in_future")
    if payload.get("schema") != RESULT_SCHEMA:
        raise ResultRejected("wrong_schema")
    if payload.get("transactionId") != transaction_id:
        raise ResultRejected("wrong_transaction")
    if not isinstance(payload["jti"], str) or not payload["jti"]:
        raise ResultRejected("invalid_jti")
