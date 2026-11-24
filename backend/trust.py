"""What the backend checked before using the wallet data, for the review screen and the jury.

Built only after every check in validator.py and the replay check have passed, so each backend line
is a fact about this result. Contains no claim values.
"""
from datetime import datetime, timezone
from typing import Optional

BACKEND_CHECKS = [
    ("sealed", "Encrypted for our bank and opened only on our backend (ECDH-ES+A256KW, A256GCM)"),
    ("signature", "Signed by the verifier, checked against its published keys (ES256)"),
    ("issuer_audience", "Issued by the expected verifier and addressed to our bank"),
    ("fresh", "Not expired and not issued in the future"),
    ("bound", "Answers our own request (transaction ID matches)"),
    ("once", "Used only once (result ID not seen before)"),
    ("outcome", "The wallet shared everything we asked for (outcome SATISFIED)"),
    ("identity", "Identity and address present"),
]
VERIFIER_CHECK = ("wallet_presentation", "Wallet presentation checked by the verifier before it signed the result")
INCOME_CHECK = "Income belongs to the same person (personal numeric code), in RON, with monthly amounts"


def build_report(credential_types: list, income_shared: bool, income_linked: bool,
                 income_note: Optional[str], checked_at: float, stub: bool = False) -> dict:
    backend = "skipped" if stub else "passed"
    checks = [{"id": check_id, "label": label, "by": "backend", "result": backend}
              for check_id, label in BACKEND_CHECKS]
    checks.insert(0, {"id": VERIFIER_CHECK[0], "label": VERIFIER_CHECK[1], "by": "verifier",
                      "result": "skipped" if stub else "passed"})
    income = {"id": "income_linked", "label": INCOME_CHECK, "by": "backend",
              "result": "not_shared" if not income_shared else ("passed" if income_linked else "failed")}
    if income_shared and not income_linked and income_note:
        income["detail"] = income_note
    checks.append(income)
    report = {"checkedAt": datetime.fromtimestamp(checked_at, timezone.utc).isoformat(timespec="seconds"),
              "credentials": [t for t in credential_types if t], "checks": checks}
    if stub:
        report["note"] = "Test data: no checks were run"
    return report
