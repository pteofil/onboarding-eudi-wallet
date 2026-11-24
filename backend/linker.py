"""Decides whether a shared income attestation belongs to the verified person and is usable."""
from typing import Optional, Tuple

from rules import reason


def link_income(pid: dict, income: dict, now: float) -> Tuple[Optional[dict], dict]:
    """Returns (income or None, reason). The reason replaces INCOME_NOT_SHARED when the income can't be used."""
    subject = (income.get("subject") or {}).get("personal_numeric_code")
    if not subject or subject != pid.get("personal_numeric_code"):
        return None, reason("INCOME_NOT_LINKED",
                            "We couldn't match your income document to your identity, so we didn't use it.")
    monthly = income.get("monthly_income") or {}
    periods = monthly.get("periods") or []
    if monthly.get("currency") != "RON" or not periods or not all(
            isinstance(p.get("net_amount_minor"), int) for p in periods):
        return None, reason("INCOME_UNUSABLE", "We couldn't read the monthly amounts in your income document, "
                                               "so we didn't use it.")
    return income, reason("INCOME_LINKED", "Income linked")
