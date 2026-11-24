"""Package recommendation from verified age and (optionally) verified income. Pure functions."""
import os
from datetime import date
from typing import Optional

LADDER = ["Simple", "Starter", "Plus", "Premium"]


def _cfg(name: str, default: int) -> int:
    return int(os.environ.get(name, default))


def age_on(birth_date: str, today: date) -> int:
    born = date.fromisoformat(birth_date)
    return today.year - born.year - ((today.month, today.day) < (born.month, born.day))


def lowest_monthly_ron(income: dict) -> float:
    return min(p["net_amount_minor"] for p in income["monthly_income"]["periods"]) / 100


def reason(code: str, text: str, **values) -> dict:
    """A reason the app shows from its own strings by `code` (Bogdan's strings.xml: reason_<code lower case>).
    `text` is the English fallback; `age` (int) and `amount` (e.g. "16,000") fill the placeholders."""
    return {"code": code, "text": text, **values}


def _ron(value: float) -> str:
    return f"{value:,.0f}"


def recommend(pid: dict, income: Optional[dict], today: date) -> dict:
    starter_max = _cfg("STARTER_MAX_AGE", 25)
    plus_min = _cfg("PLUS_MIN_MONTHLY_RON", 5000)
    premium_min = _cfg("PREMIUM_MIN_MONTHLY_RON", 10000)
    travel_max = _cfg("TRAVEL_INSURANCE_MAX_AGE", 70)
    life_max = _cfg("LIFE_INSURANCE_MAX_AGE", 80)

    age = age_on(pid.get("birth_date") or pid["birthdate"], today)
    assumptions = [reason("FICTIONAL", "Prices and benefits belong to a fictional bank and are for demonstration only.")]
    if age < 18:
        return {"status": "NOT_ELIGIBLE", "recommended": None, "eligible": [], "notEligible": [],
                "insurance": {}, "assumptions": assumptions,
                "reasons": [reason("AGE_UNDER_18", "You need to be 18 to open an account on your own.", age=age)]}

    eligible, not_eligible = ["Simple"], []
    reasons = [reason("AGE_ADULT", f"You're {age}, so you can open an account on your own.", age=age)]

    if age <= starter_max:
        eligible.append("Starter")
        reasons.append(reason("STARTER_AGE", f"You're {age}, so Starter is free for you.", age=age))
    else:
        not_eligible.append({"package": "Starter",
                             **reason("STARTER_TOO_OLD", f"Starter is for ages 18–{starter_max}.")})

    tiers = [("Plus", plus_min), ("Premium", premium_min)]
    monthly = lowest_monthly_ron(income) if income is not None else None
    for name, minimum in tiers:
        if monthly is not None and monthly >= minimum:
            eligible.append(name)
        else:
            not_eligible.append({"package": name, **reason(
                f"NEEDS_{name.upper()}", f"{name} needs a net income of at least {_ron(minimum)} RON a month.",
                amount=_ron(minimum))})

    if monthly is None:
        reasons.append(reason("INCOME_NOT_SHARED",
                              "You didn't share your income, so we've shown packages that don't need it."))
    else:
        if eligible[-1] in ("Plus", "Premium"):
            reasons.append(reason(f"INCOME_{eligible[-1].upper()}",
                                  f"Your verified net income (lowest month: {_ron(monthly)} RON) qualifies you "
                                  f"for {eligible[-1]}.", amount=_ron(monthly)))
        reasons.append(reason("LOWEST_MONTH", f"We used your lowest month of the last six ({_ron(monthly)} RON net).",
                              amount=_ron(monthly)))
        assumptions.append(reason("INCOME", "We used your net income for the last six months; "
                                            "this isn't a credit assessment."))

    recommended = max(eligible, key=LADDER.index)
    insurance = {}
    if recommended == "Premium":
        insurance = {
            "travel": _insurance("TRAVEL", "Travel", age, travel_max),
            "life": _insurance("LIFE", "Life", age, life_max),
        }
        assumptions.append(reason("INSURANCE", "Insurance availability is based on your age today."))
    return {"status": "ELIGIBLE", "recommended": recommended, "eligible": eligible,
            "notEligible": not_eligible, "insurance": insurance, "reasons": reasons,
            "assumptions": assumptions}


def _insurance(code: str, label: str, age: int, max_age: int) -> dict:
    if age < max_age:
        return {"available": True,
                **reason(f"{code}_INS_OK", f"{label} insurance is included (available up to age {max_age}).")}
    return {"available": False,
            **reason(f"{code}_INS_AGE", f"{label} insurance isn't available at your age (up to age {max_age}).")}
