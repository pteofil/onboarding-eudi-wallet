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


def recommend(pid: dict, income: Optional[dict], today: date) -> dict:
    starter_max = _cfg("STARTER_MAX_AGE", 25)
    plus_min = _cfg("PLUS_MIN_MONTHLY_RON", 5000)
    premium_min = _cfg("PREMIUM_MIN_MONTHLY_RON", 10000)
    travel_max = _cfg("TRAVEL_INSURANCE_MAX_AGE", 70)
    life_max = _cfg("LIFE_INSURANCE_MAX_AGE", 80)

    age = age_on(pid.get("birth_date") or pid["birthdate"], today)
    assumptions = [f"Age is counted on {today.isoformat()}"]
    if age < 18:
        return {"status": "NOT_ELIGIBLE", "recommended": None, "eligible": [], "notEligible": [],
                "insurance": {}, "reasons": ["You need to be 18 to open an account on your own"],
                "assumptions": assumptions}

    eligible, not_eligible = ["Simple"], []
    reasons = [f"You're {age}, so you can open an account"]

    if age <= starter_max:
        eligible.append("Starter")
        reasons.append(f"You're {age} → Starter (up to age {starter_max})")
    else:
        not_eligible.append({"package": "Starter", "reason": f"Starter is for ages 18–{starter_max}"})

    tiers = [("Plus", plus_min), ("Premium", premium_min)]
    if income is None:
        for name, minimum in tiers:
            not_eligible.append({"package": name,
                                 "reason": f"Share your income to unlock {name} (from {minimum:,} RON/month)"})
    else:
        monthly = lowest_monthly_ron(income)
        assumptions.append("Income = the lowest net monthly amount of the last 6 months")
        for name, minimum in tiers:
            if monthly >= minimum:
                eligible.append(name)
                reasons.append(f"Your verified income of {monthly:,.0f} RON/month → {name}")
            else:
                not_eligible.append({"package": name, "reason": f"{name} needs at least {minimum:,} RON/month"})

    recommended = max(eligible, key=LADDER.index)
    insurance = {}
    if recommended == "Premium":
        insurance = {
            "travel": _insurance("Travel", age, travel_max),
            "life": _insurance("Life", age, life_max),
        }
        assumptions.append("Insurance age limits apply at policy start (today)")
    return {"status": "ELIGIBLE", "recommended": recommended, "eligible": eligible,
            "notEligible": not_eligible, "insurance": insurance, "reasons": reasons,
            "assumptions": assumptions}


def _insurance(label: str, age: int, max_age: int) -> dict:
    if age < max_age:
        return {"available": True, "reason": f"{label} insurance is included (available up to age {max_age})"}
    return {"available": False, "reason": f"{label} insurance is not available from age {max_age}"}
