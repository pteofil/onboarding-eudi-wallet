import json
from datetime import date

import pytest
from jwcrypto import jwk

from handler import build_service, lambda_handler, provisional_link_income, provisional_recommend
from service import OnboardingService
from store import MemoryStore
from test_validator import AUDIENCE, ISSUER, NOW, seal
from validator import open_result
from verifier_client import VerifierError

PID = {"given_name": "ANA", "family_name": "POPESCU", "birth_date": "1996-03-14",
       "personal_numeric_code": "2960314123456"}
ADDRESS = {"locality": "Bucuresti"}


class FakeVerifier:
    def __init__(self, sealed_results, wallet_status="COMPLETED", needs_claim=False):
        self.sealed_results, self.wallet_status, self.needs_claim = list(sealed_results), wallet_status, needs_claim
        self.acknowledged = []

    def start(self, profile, return_uri=None):
        self.return_uri = return_uri
        return {"transactionId": "tx-1", "deepLink": "openid4vp://request"}

    def status(self, tx):
        if self.needs_claim:  # the real verifier also blocks status until the app passes the response code
            raise VerifierError(409, "RESPONSE_CODE_REQUIRED")
        return {"status": self.wallet_status}

    def claim(self, tx, code):
        self.needs_claim = False

    def collect(self, tx):
        if self.needs_claim:
            raise VerifierError(409, "RESPONSE_CODE_REQUIRED")
        return self.sealed_results.pop(0)

    def acknowledge(self, tx):
        self.acknowledged.append(tx)


@pytest.fixture(scope="module")
def keys():
    signing = jwk.JWK.generate(kty="EC", crv="P-256", kid="verifier-1")
    result_key = jwk.JWK.generate(kty="EC", crv="P-256", kid="bank-1")
    return signing, result_key, {"keys": [json.loads(signing.export_public())]}


def sealed(keys, jti="jti-1", status="SATISFIED", credentials=None):
    signing, result_key, _ = keys
    body = {"iss": ISSUER, "aud": AUDIENCE, "iat": NOW - 5, "exp": NOW + 300, "jti": jti,
            "schema": "eudi-verification-result/v1", "transactionId": "tx-1", "outcome": {"status": status},
            "credentials": credentials if credentials is not None else
            [{"type": "PID_RO", "claims": PID}, {"type": "ADDRESS", "claims": ADDRESS}]}
    return seal(body, signing, result_key)


def service(keys, verifier, store=None, return_uri=None):
    _, result_key, jwks = keys

    def open_sealed(token, tx):
        return open_result(token, result_key, jwks, ISSUER, AUDIENCE, tx, now=NOW)

    return OnboardingService(store or MemoryStore(), verifier, open_sealed, "BASIC", "FULL", return_uri,
                             provisional_recommend, provisional_link_income,
                             clock=lambda: NOW, today=lambda: date(2026, 11, 24))


def test_verified_result_reaches_review_and_is_acknowledged(keys):
    verifier = FakeVerifier([sealed(keys)])
    svc = service(keys, verifier)
    app_id = svc.create(share_income=False)["applicationId"]

    assert svc.status(app_id)["status"] == "VERIFIED"
    review = svc.review(app_id)
    assert review["identity"]["family_name"] == "POPESCU"
    assert review["personalNumericCode"] == "2********3456"
    assert review["decision"]["recommended"] == "Simple"
    assert verifier.acknowledged == ["tx-1"]
    results = {check["id"]: check["result"] for check in review["trust"]["checks"]}
    assert results.pop("income_linked") == "not_shared"
    assert set(results.values()) == {"passed"}
    assert review["trust"]["credentials"] == ["PID_RO", "ADDRESS"]
    assert "POPESCU" not in json.dumps(review["trust"]) and PID["personal_numeric_code"] not in json.dumps(review["trust"])


def test_replayed_result_is_rejected(keys):
    store = MemoryStore()
    token = sealed(keys, jti="same-jti")
    first = service(keys, FakeVerifier([token]), store)
    second = service(keys, FakeVerifier([token]), store)

    assert first.status(first.create(False)["applicationId"])["status"] == "VERIFIED"
    assert second.status(second.create(False)["applicationId"])["status"] == "NOT_VERIFIED"


def test_tampered_result_is_not_verified(keys):
    svc = service(keys, FakeVerifier(["garbage"]))
    assert svc.status(svc.create(False)["applicationId"])["status"] == "NOT_VERIFIED"


def test_same_device_result_waits_for_claim(keys):
    verifier = FakeVerifier([sealed(keys)], needs_claim=True)
    svc = service(keys, verifier, return_uri="app://cb")
    app_id = svc.create(False, return_to_app=True)["applicationId"]

    assert verifier.return_uri == "app://cb"
    assert svc.status(app_id) == {"applicationId": app_id, "status": "PENDING",
                                  "walletStatus": "WAITING_FOR_APP_RETURN"}
    assert svc.claim(app_id, "code-1")["status"] == "VERIFIED"


def test_return_to_app_is_opt_in(keys):
    verifier = FakeVerifier([sealed(keys)])
    service(keys, verifier, return_uri="app://cb").create(False)
    assert verifier.return_uri is None


@pytest.mark.parametrize("wallet_status, expected", [
    ("DECLINED", "DECLINED"), ("EXPIRED", "EXPIRED"), ("WALLET_ERROR", "WALLET_ERROR"),
])
def test_wallet_failures(keys, wallet_status, expected):
    svc = service(keys, FakeVerifier([], wallet_status=wallet_status))
    assert svc.status(svc.create(False)["applicationId"])["status"] == expected


@pytest.mark.parametrize("status, expected", [("REFUSED", "DECLINED"), ("NOT_SATISFIED", "NOT_VERIFIED")])
def test_unsatisfied_outcomes(keys, status, expected):
    svc = service(keys, FakeVerifier([sealed(keys, status=status)]))
    assert svc.status(svc.create(False)["applicationId"])["status"] == expected


def test_missing_address_is_reported(keys):
    svc = service(keys, FakeVerifier([sealed(keys, credentials=[{"type": "PID_RO", "claims": PID}])]))
    assert svc.status(svc.create(False)["applicationId"])["status"] == "MISSING_IDENTITY"


def test_sandbox_pid_birthdate_attribute_is_used(keys):
    sandbox_pid = {k: v for k, v in PID.items() if k != "birth_date"} | {"birthdate": "2010-01-01"}
    credentials = [{"type": "PID_RO_SANDBOX", "claims": sandbox_pid},
                   {"type": "ADDRESS", "claims": {"address": {"formatted": "Str. X 1, Bucuresti"}}}]
    svc = service(keys, FakeVerifier([sealed(keys, credentials=credentials)]))
    assert svc.status(svc.create(False)["applicationId"])["status"] == "UNDER_18"


def test_income_of_someone_else_is_ignored(keys):
    income = {"subject": {"personal_numeric_code": "1800101000000"}, "monthly_income": {"periods": []}}
    credentials = [{"type": "PID_RO", "claims": PID}, {"type": "ADDRESS", "claims": ADDRESS},
                   {"type": "INCOME", "claims": income}]
    svc = service(keys, FakeVerifier([sealed(keys, credentials=credentials)]))
    app_id = svc.create(True)["applicationId"]

    assert svc.status(app_id)["status"] == "VERIFIED"
    review = svc.review(app_id)
    assert review["incomeShared"] is False
    income_check = review["trust"]["checks"][-1]
    assert income_check["result"] == "failed" and "someone else" in income_check["detail"]


def test_confirm_only_eligible_package(keys):
    svc = service(keys, FakeVerifier([sealed(keys)]))
    app_id = svc.create(False)["applicationId"]
    svc.status(app_id)

    with pytest.raises(Exception, match="not eligible"):
        svc.confirm(app_id, "Premium")
    confirmed = svc.confirm(app_id, None)
    assert confirmed["package"] == "Simple" and confirmed["iban"].startswith("RO")


def test_stub_flow_through_lambda_handler(monkeypatch):
    import handler

    monkeypatch.setattr(handler, "_service", build_service(
        {"STUB_VERIFIER": "1", "VERIFIER_RETURN_URI": "ro.banking40.team01://same-device",
         "PROFILE_BASIC": "onboard-basic", "PROFILE_FULL": "onboard-full"}))

    def call(method, path, body=None):
        response = lambda_handler({"rawPath": path, "body": json.dumps(body) if body else None,
                                   "requestContext": {"http": {"method": method}}})
        return response["statusCode"], json.loads(response["body"])

    _, created = call("POST", "/applications", {"shareIncome": True, "returnToApp": True})
    assert created["deepLink"].startswith("ro.banking40.team01://same-device#response_code=")
    app = f"/applications/{created['applicationId']}"
    assert call("POST", f"{app}/claim", {"responseCode": "stub-code"})[1]["status"] == "VERIFIED"
    assert call("GET", f"{app}/review")[1]["incomeShared"] is True
    assert call("POST", f"{app}/confirm", {})[1]["status"] == "CONFIRMED"
    assert call("GET", "/nope")[0] == 404
    assert call("GET", "/applications/unknown/status")[0] == 404


def test_stub_on_request_alongside_real_verifier(monkeypatch):
    import base64

    import handler

    key = jwk.JWK.generate(kty="EC", crv="P-256").export_to_pem(private_key=True, password=None)
    env = {"VERIFIER_BASE_URL": "https://verifier.invalid", "VERIFIER_WEBSITE_KEY": "k", "VERIFIER_WEBSITE_SECRET": "s",
           "VERIFIER_AUDIENCE": "aud", "RESULT_PRIVATE_KEY_B64": base64.b64encode(key).decode(),
           "PROFILE_BASIC": "onboard-basic", "PROFILE_FULL": "onboard-full"}

    def call(body, path="/applications", method="POST"):
        response = lambda_handler({"rawPath": path, "body": json.dumps(body) if body else None,
                                   "requestContext": {"http": {"method": method}}})
        return response["statusCode"], json.loads(response["body"])

    monkeypatch.setattr(handler, "_service", build_service(env))
    assert call({"shareIncome": True, "stub": True})[0] == 400

    monkeypatch.setattr(handler, "_service", build_service({**env, "ALLOW_STUB": "1"}))
    _, created = call({"shareIncome": True, "stub": True})
    app_id = created["applicationId"]
    assert call(None, f"/applications/{app_id}/status", "GET")[1]["status"] == "VERIFIED"
    review = call(None, f"/applications/{app_id}/review", "GET")[1]
    assert review["stub"] is True and review["incomeShared"] is True
    assert review["trust"]["note"] == "Test data: no checks were run"
    assert {c["result"] for c in review["trust"]["checks"] if c["id"] != "income_linked"} == {"skipped"}
