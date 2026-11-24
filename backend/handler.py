"""Lambda Function URL entry point (also used by local_server.py)."""
from __future__ import annotations

import base64
import json
import logging
import os
import re
import time
from datetime import date
from typing import Optional

from service import BadRequest, NotFound, OnboardingService
from verifier_client import VerifierError

logging.getLogger().setLevel(logging.INFO)
log = logging.getLogger("verifiedstart")

ROUTES = [
    ("POST", re.compile(r"^/applications$"), "create"),
    ("POST", re.compile(r"^/applications/([\w-]+)/claim$"), "claim"),
    ("GET", re.compile(r"^/applications/([\w-]+)/status$"), "status"),
    ("GET", re.compile(r"^/applications/([\w-]+)/review$"), "review"),
    ("POST", re.compile(r"^/applications/([\w-]+)/confirm$"), "confirm"),
]

_service: Optional[OnboardingService] = None


def lambda_handler(event, context=None):
    http = event.get("requestContext", {}).get("http", {})
    method, path = http.get("method", "GET"), event.get("rawPath", "/")
    body = event.get("body") or ""
    if event.get("isBase64Encoded"):
        body = base64.b64decode(body).decode()
    try:
        data = json.loads(body) if body else {}
    except ValueError:
        return _response(400, {"error": "invalid_json"})
    try:
        return _response(200, route(get_service(), method, path, data))
    except NotFound:
        return _response(404, {"error": "not_found"})
    except BadRequest as error:
        return _response(400, {"error": str(error)})
    except VerifierError as error:
        log.warning("verifier error %s %s on %s %s", error.status, error.code, method, path)
        return _response(502, {"error": "verifier_unavailable"})


def route(service: OnboardingService, method: str, path: str, data: dict) -> dict:
    for route_method, pattern, action in ROUTES:
        match = pattern.match(path)
        if match and method == route_method:
            if action == "create":
                return service.create(bool(data.get("shareIncome")), bool(data.get("returnToApp")))
            app_id = match.group(1)
            if action == "claim":
                return service.claim(app_id, data.get("responseCode", ""))
            if action == "confirm":
                return service.confirm(app_id, data.get("package"))
            return getattr(service, action)(app_id)
    raise NotFound(path)


def _response(status: int, body: dict) -> dict:
    return {"statusCode": status, "headers": {"Content-Type": "application/json"}, "body": json.dumps(body)}


def get_service() -> OnboardingService:
    global _service
    if _service is None:
        _service = build_service(os.environ)
    return _service


def build_service(env) -> OnboardingService:
    from store import DynamoStore, MemoryStore

    store = DynamoStore(env["TABLE_NAME"]) if env.get("USE_DYNAMO") == "1" else MemoryStore()
    if env.get("STUB_VERIFIER") == "1":
        from stub_verifier import StubVerifier

        verifier, open_sealed = StubVerifier(), lambda sealed, tx: json.loads(sealed)
        log.warning("STUB VERIFIER: results are NOT verified")
    else:
        verifier, open_sealed = _real_verifier(env)
    recommend, link_income = _rules()
    return OnboardingService(
        store, verifier, open_sealed, env.get("PROFILE_BASIC", "ONBOARD_BASIC"),
        env.get("PROFILE_FULL", "ONBOARD_FULL"), env.get("VERIFIER_RETURN_URI") or None,
        recommend, link_income,
    )


def _real_verifier(env):
    from validator import load_private_key, open_result
    from verifier_client import VerifierClient

    client = VerifierClient(env["VERIFIER_BASE_URL"], env["VERIFIER_WEBSITE_KEY"], env["VERIFIER_WEBSITE_SECRET"])
    private_key = load_private_key(base64.b64decode(env["RESULT_PRIVATE_KEY_B64"]))
    jwks_cache = {}

    def open_sealed(sealed: str, transaction_id: str) -> dict:
        if "keys" not in jwks_cache:
            jwks_cache.update(client.jwks())
        return open_result(sealed, private_key, jwks_cache, env["VERIFIER_BASE_URL"],
                           env["VERIFIER_AUDIENCE"], transaction_id)

    return client, open_sealed


def _rules():
    try:
        from rules import recommend
    except ImportError:
        recommend = provisional_recommend
    try:
        from linker import link_income
    except ImportError:
        link_income = provisional_link_income
    return recommend, link_income


def provisional_recommend(pid: dict, income: Optional[dict], today: date) -> dict:
    """Placeholder until rules.py lands: age gate only, everyone adult gets Simple."""
    born = date.fromisoformat(pid["birth_date"])
    age = today.year - born.year - ((today.month, today.day) < (born.month, born.day))
    if age < 18:
        return {"status": "NOT_ELIGIBLE", "recommended": None, "eligible": [], "notEligible": [],
                "reasons": ["You need to be 18 to open an account on your own"], "assumptions": []}
    return {"status": "ELIGIBLE", "recommended": "Simple", "eligible": ["Simple"], "notEligible": [],
            "insurance": {}, "reasons": [f"You're {age} → Simple"], "assumptions": ["provisional rules"]}


def provisional_link_income(pid: dict, income: dict, now: float):
    """Placeholder until linker.py lands: CNP match only."""
    subject = (income.get("subject") or {}).get("personal_numeric_code")
    if subject and subject == pid.get("personal_numeric_code"):
        return income, "income linked"
    return None, "Income document belongs to someone else"
