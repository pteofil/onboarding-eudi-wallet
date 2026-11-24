"""Decides whether a shared income attestation belongs to the verified person and is usable."""
from typing import Optional, Tuple


def link_income(pid: dict, income: dict, now: float) -> Tuple[Optional[dict], str]:
    subject = (income.get("subject") or {}).get("personal_numeric_code")
    if not subject or subject != pid.get("personal_numeric_code"):
        return None, "The income document belongs to someone else, so we can't use it"
    monthly = income.get("monthly_income") or {}
    if monthly.get("currency") != "RON":
        return None, "We can only use income paid in RON"
    periods = monthly.get("periods") or []
    if not periods or not all(isinstance(p.get("net_amount_minor"), int) for p in periods):
        return None, "The income document has no usable monthly amounts"
    return income, "income linked"
