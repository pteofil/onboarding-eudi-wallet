"""Fake verifier for building the app without Tremend (STUB_VERIFIER=1).

The deepLink points straight back to the app's callback, so the whole hand-off can be
tested on a phone. Results are plain JSON and skip the trust checks; never use in a demo.
"""
from __future__ import annotations

import json
import uuid

STUB_PID = {
    "given_name": "ANA", "family_name": "POPESCU", "birth_date": "1996-03-14",
    "personal_numeric_code": "2960314123456",
}
STUB_ADDRESS = {"street": "Str. Exemplu 1", "locality": "Bucuresti", "postal_code": "010101", "country": "RO"}
STUB_INCOME = {
    "subject": {"personal_numeric_code": STUB_PID["personal_numeric_code"]},
    "monthly_income": {"periods": [{"month": f"2026-0{m}", "net_amount_minor": 1_200_000} for m in range(4, 10)]},
}


class StubVerifier:
    def __init__(self):
        self._tx: dict = {}

    def start(self, profile: str, return_uri: str | None = None) -> dict:
        tx = f"stub-{uuid.uuid4()}"
        self._tx[tx] = {"profile": profile, "claimed": False}
        return {"transactionId": tx, "deepLink": f"{return_uri}#response_code=stub-code"}

    def claim(self, transaction_id: str, response_code: str) -> dict:
        self._tx[transaction_id]["claimed"] = True
        return {}

    def status(self, transaction_id: str) -> dict:
        claimed = self._tx.get(transaction_id, {}).get("claimed")
        return {"status": "COMPLETED" if claimed else "REQUEST_RETRIEVED"}

    def collect(self, transaction_id: str) -> str:
        credentials = [{"type": "PID_RO", "claims": STUB_PID}, {"type": "ADDRESS_RO", "claims": STUB_ADDRESS}]
        if self._tx[transaction_id]["profile"].endswith("FULL"):
            credentials.append({"type": "INCOME_RO", "claims": STUB_INCOME})
        return json.dumps({
            "jti": f"stub-{uuid.uuid4()}", "transactionId": transaction_id,
            "outcome": {"status": "SATISFIED", "withheld": []}, "credentials": credentials,
        })

    def acknowledge(self, transaction_id: str) -> None:
        self._tx.pop(transaction_id, None)
