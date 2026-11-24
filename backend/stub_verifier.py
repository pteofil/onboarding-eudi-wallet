"""Fake verifier for building the app without Tremend (STUB_VERIFIER=1).

The deepLink points straight back to the app's callback, so the whole hand-off can be
tested on a phone. Results are plain JSON and skip the trust checks; never use in a demo.
"""
from __future__ import annotations

import json
import uuid

# Same shapes as the real sandbox credentials (PID_RO_SANDBOX, ADDRESS, INCOME_ATTESTATION).
STUB_PID = {
    "given_name": "ANA", "family_name": "POPESCU", "birthdate": "1996-03-14",
    "age_equal_or_over": {"18": True}, "place_of_birth": {"country": "RO", "locality": "Bucuresti"},
    "personal_numeric_code": "2960314123456",
}
STUB_ADDRESS = {"address": {"formatted": "Str. Exemplu 1, 010101 Bucuresti, Romania"}}
STUB_INCOME = {
    "subject": {"personal_numeric_code": STUB_PID["personal_numeric_code"]},
    "monthly_income": {"income_type": "SALARY", "currency": "RON",
                       "periods": [{"month": f"2026-0{m}", "net_amount_minor": 1_200_000} for m in range(4, 10)]},
}


class StubVerifier:
    """Stateless (Lambda instances don't share memory): the profile is encoded in the transaction id."""

    accepts_stub = True

    def start(self, profile: str, return_uri: str | None = None, stub: bool = True) -> dict:
        tx = f"stub-{'full' if profile.lower().endswith('full') else 'basic'}-{uuid.uuid4()}"
        deep_link = f"{return_uri}#response_code=stub-code" if return_uri else "openid4vp://stub"
        return {"transactionId": tx, "deepLink": deep_link}

    def claim(self, transaction_id: str, response_code: str) -> dict:
        return {}

    def status(self, transaction_id: str) -> dict:
        return {"status": "COMPLETED"}

    def collect(self, transaction_id: str) -> str:
        credentials = [{"type": "PID_RO_SANDBOX", "claims": STUB_PID}, {"type": "ADDRESS", "claims": STUB_ADDRESS}]
        if transaction_id.startswith("stub-full-"):
            credentials.append({"type": "INCOME_ATTESTATION", "claims": STUB_INCOME})
        return json.dumps({
            "jti": f"stub-{uuid.uuid4()}", "transactionId": transaction_id,
            "outcome": {"status": "SATISFIED", "withheld": []}, "credentials": credentials,
        })

    def acknowledge(self, transaction_id: str) -> None:
        pass
