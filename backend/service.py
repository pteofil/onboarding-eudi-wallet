"""Onboarding flow: start a wallet request, collect and trust the result, decide, confirm.

Only `outcome` and display fields leave this module; claim values are never logged.
"""
from __future__ import annotations

import logging
import random
import time
import uuid
from datetime import date
from typing import Callable, Optional

from validator import ResultRejected
from verifier_client import VerifierError

log = logging.getLogger("verifiedstart")

APP_TTL_S = 24 * 3600
JTI_TTL_S = 2 * 24 * 3600
FINAL = {"VERIFIED", "CONFIRMED", "DECLINED", "EXPIRED", "WALLET_ERROR", "NOT_VERIFIED",
         "UNDER_18", "MISSING_IDENTITY"}
WALLET_FAILURES = {"DECLINED": "DECLINED", "EXPIRED": "EXPIRED", "WALLET_ERROR": "WALLET_ERROR",
                   "APPROVED": "NOT_VERIFIED", "REJECTED": "NOT_VERIFIED"}


class NotFound(Exception):
    pass


class BadRequest(Exception):
    pass


class OnboardingService:
    def __init__(self, store, verifier, open_sealed: Callable[[str, str], dict], profile_basic: str,
                 profile_full: str, return_uri: Optional[str], recommend: Callable, link_income: Callable,
                 clock: Callable[[], float] = time.time, today: Callable[[], date] = date.today):
        self.store, self.verifier, self.open_sealed = store, verifier, open_sealed
        self.profile_basic, self.profile_full, self.return_uri = profile_basic, profile_full, return_uri
        self.recommend, self.link_income, self.clock, self.today = recommend, link_income, clock, today

    def create(self, share_income: bool) -> dict:
        profile = self.profile_full if share_income else self.profile_basic
        started = self.verifier.start(profile, self.return_uri)
        app_id = str(uuid.uuid4())
        app = {"id": app_id, "state": "PENDING", "transactionId": started["transactionId"],
               "shareIncome": share_income, "createdAt": int(self.clock())}
        self._save(app)
        log.info("application %s started tx=%s profile=%s", app_id, started["transactionId"], profile)
        return {"applicationId": app_id, "deepLink": started["deepLink"], "status": "PENDING"}

    def claim(self, app_id: str, response_code: str) -> dict:
        if not response_code:
            raise BadRequest("responseCode required")
        app = self._load(app_id)
        if app["state"] == "PENDING":
            self.verifier.claim(app["transactionId"], response_code)
        return self.status(app_id)

    def status(self, app_id: str) -> dict:
        app = self._load(app_id)
        if app["state"] == "PENDING":
            self._poll(app)
        view = {"applicationId": app_id, "status": app["state"]}
        if app.get("walletStatus") and app["state"] == "PENDING":
            view["walletStatus"] = app["walletStatus"]
        return view

    def review(self, app_id: str) -> dict:
        app = self._load(app_id)
        if app["state"] != "VERIFIED":
            raise BadRequest(f"application is {app['state']}")
        return {"applicationId": app_id, **app["display"], "decision": app["decision"]}

    def confirm(self, app_id: str, package: Optional[str]) -> dict:
        app = self._load(app_id)
        if app["state"] != "VERIFIED":
            raise BadRequest(f"application is {app['state']}")
        chosen = package or app["decision"]["recommended"]
        if chosen not in app["decision"].get("eligible", []):
            raise BadRequest("package not eligible")
        app.update(state="CONFIRMED", package=chosen, display=None,
                   iban="RO" + "".join(random.choices("0123456789", k=2)) + "DEMO" + "".join(random.choices("0123456789", k=16)),
                   reference=f"VS-{app_id[:8].upper()}")
        self._save(app)
        log.info("application %s confirmed package=%s", app_id, chosen)
        return {"applicationId": app_id, "status": "CONFIRMED", "package": chosen,
                "iban": app["iban"], "reference": app["reference"]}

    def _poll(self, app: dict) -> None:
        tx = app["transactionId"]
        try:
            wallet_status = self.verifier.status(tx).get("status")
        except VerifierError as error:
            log.warning("application %s status call failed %s", app["id"], error.code)
            return
        if wallet_status == "COMPLETED":
            self._complete(app)
        elif wallet_status in WALLET_FAILURES:
            self._finish(app, WALLET_FAILURES[wallet_status], wallet_status)
        elif wallet_status != app.get("walletStatus"):
            app["walletStatus"] = wallet_status
            self._save(app)

    def _complete(self, app: dict) -> None:
        tx = app["transactionId"]
        try:
            sealed = self.verifier.collect(tx)
        except VerifierError as error:
            if error.code == "RESPONSE_CODE_REQUIRED":
                app["walletStatus"] = "WAITING_FOR_APP_RETURN"
                self._save(app)
                return
            log.warning("application %s collect failed %s", app["id"], error.code)
            return
        try:
            payload = self.open_sealed(sealed, tx)
        except ResultRejected as rejected:
            return self._finish(app, "NOT_VERIFIED", rejected.reason)
        now = int(self.clock())
        if not self.store.put_new(f"jti#{payload['jti']}", {"tx": tx}, now + JTI_TTL_S):
            return self._finish(app, "NOT_VERIFIED", "replayed_jti")
        try:
            self.verifier.acknowledge(tx)
        except VerifierError as error:
            log.warning("application %s acknowledge failed %s", app["id"], error.code)
        self._apply(app, payload)

    def _apply(self, app: dict, payload: dict) -> None:
        outcome = payload.get("outcome", {})
        if outcome.get("status") == "REFUSED":
            return self._finish(app, "DECLINED", "refused")
        if outcome.get("status") != "SATISFIED":
            return self._finish(app, "NOT_VERIFIED", "requirement_unmet")
        pid, address, income = split_credentials(payload.get("credentials", []))
        if not pid or not pid.get("birth_date") or not address:
            return self._finish(app, "MISSING_IDENTITY", "pid_or_address_missing")
        linked_income, income_note = (None, "income not shared")
        if income:
            linked_income, income_note = self.link_income(pid, income, self.clock())
        decision = self.recommend(pid, linked_income, self.today())
        if income and not linked_income:
            decision.setdefault("reasons", []).append(income_note)
        app["decision"] = decision
        app["display"] = {
            "identity": {k: pid.get(k) for k in ("given_name", "family_name", "birth_date")},
            "personalNumericCode": mask(pid.get("personal_numeric_code")),
            "address": address,
            "incomeShared": linked_income is not None,
            "withheld": outcome.get("withheld", []),
        }
        state = "UNDER_18" if decision.get("status") == "NOT_ELIGIBLE" else "VERIFIED"
        self._finish(app, state, "satisfied")

    def _finish(self, app: dict, state: str, reason: str) -> None:
        app.update(state=state, reason=reason)
        app.pop("walletStatus", None)
        self._save(app)
        log.info("application %s -> %s (%s)", app["id"], state, reason)

    def _load(self, app_id: str) -> dict:
        app = self.store.get(f"app#{app_id}")
        if not app:
            raise NotFound(app_id)
        return app

    def _save(self, app: dict) -> None:
        self.store.put(f"app#{app['id']}", app, app["createdAt"] + APP_TTL_S)


def split_credentials(credentials: list) -> tuple:
    """Find PID, address and income by their fields; attribute names per type are not documented."""
    pid = address = income = None
    for credential in credentials:
        claims = credential.get("claims") or {}
        if "monthly_income" in claims:
            income = claims
        elif "birthdate" in claims or "birth_date" in claims or "PID" in str(credential.get("type", "")).upper():
            pid = dict(claims)
        else:
            address = claims
    if pid and "birth_date" not in pid and "birthdate" in pid:
        pid["birth_date"] = pid["birthdate"]  # sandbox PID uses `birthdate`; rules use `birth_date`
    return pid, address, income


def mask(cnp: Optional[str]) -> Optional[str]:
    return f"{cnp[0]}********{cnp[-4:]}" if cnp and len(cnp) >= 5 else None
