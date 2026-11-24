from datetime import date

import pytest

from linker import link_income
from rules import recommend

TODAY = date(2026, 11, 24)
CNP = "1900101123456"


def pid(age: int, days_before_birthday: int = 0) -> dict:
    born = date(TODAY.year - age, TODAY.month, TODAY.day)
    if days_before_birthday:
        born = date(born.year - 1, born.month, born.day + days_before_birthday)
    return {"birth_date": born.isoformat(), "personal_numeric_code": CNP}


def income(monthly_ron: int, lowest_ron: int = None, cnp: str = CNP, currency: str = "RON") -> dict:
    amounts = [monthly_ron * 100] * 5 + [(lowest_ron or monthly_ron) * 100]
    return {"subject": {"personal_numeric_code": cnp},
            "monthly_income": {"income_type": "salary", "currency": currency,
                               "periods": [{"month": f"2026-0{i + 1}", "net_amount_minor": a}
                                           for i, a in enumerate(amounts)]}}


@pytest.mark.parametrize("age,status", [(17, "NOT_ELIGIBLE"), (18, "ELIGIBLE")])
def test_adult_gate(age, status):
    assert recommend(pid(age), None, TODAY)["status"] == status


def test_turns_18_tomorrow_is_still_minor():
    assert recommend(pid(17, days_before_birthday=1), None, TODAY)["status"] == "NOT_ELIGIBLE"


@pytest.mark.parametrize("age,expected", [(25, "Starter"), (26, "Simple")])
def test_starter_age_boundary(age, expected):
    assert recommend(pid(age), None, TODAY)["recommended"] == expected


@pytest.mark.parametrize("monthly,expected", [(4999, "Simple"), (5000, "Plus"), (9999, "Plus"), (10000, "Premium")])
def test_income_thresholds(monthly, expected):
    assert recommend(pid(40), income(monthly), TODAY)["recommended"] == expected


def test_lowest_month_counts():
    assert recommend(pid(40), income(12000, lowest_ron=9000), TODAY)["recommended"] == "Plus"


def test_young_high_earner_gets_premium_over_starter():
    out = recommend(pid(24), income(10000), TODAY)
    assert out["recommended"] == "Premium" and "Starter" in out["eligible"]


@pytest.mark.parametrize("age,travel,life", [(69, True, True), (70, False, True), (79, False, True), (80, False, False)])
def test_premium_insurance_age_limits(age, travel, life):
    ins = recommend(pid(age), income(10000), TODAY)["insurance"]
    assert (ins["travel"]["available"], ins["life"]["available"]) == (travel, life)


def test_thresholds_configurable(monkeypatch):
    monkeypatch.setenv("PREMIUM_MIN_MONTHLY_RON", "20000")
    assert recommend(pid(40), income(10000), TODAY)["recommended"] == "Plus"


def test_income_not_shared_explains_how_to_unlock():
    out = recommend(pid(40), None, TODAY)
    assert [n["code"] for n in out["notEligible"]] == ["STARTER_TOO_OLD", "NEEDS_PLUS", "NEEDS_PREMIUM"]
    assert [r["code"] for r in out["reasons"]] == ["AGE_ADULT", "INCOME_NOT_SHARED"]


def test_variable_income_uses_lowest_month():
    # Bogdan's "variable" scenario: the average (6,100) would reach Plus, the lowest month (1,900) doesn't.
    amounts = [2400, 11200, 3800, 9200, 1900, 8100]
    inc = income(0)
    inc["monthly_income"]["periods"] = [{"month": f"2026-0{i + 1}", "net_amount_minor": a * 100}
                                        for i, a in enumerate(amounts)]
    out = recommend(pid(35), inc, TODAY)
    assert out["recommended"] == "Simple"
    assert out["reasons"][-1] == {"code": "LOWEST_MONTH", "amount": "1,900",
                                  "text": "We used your lowest month of the last six (1,900 RON net)."}


def test_premium_reasons_carry_amount_and_insurance_codes():
    out = recommend(pid(35), income(16000), TODAY)
    assert out["reasons"][1] == {"code": "INCOME_PREMIUM", "amount": "16,000", "text": out["reasons"][1]["text"]}
    assert (out["insurance"]["travel"]["code"], out["insurance"]["life"]["code"]) == ("TRAVEL_INS_OK", "LIFE_INS_OK")


def test_linker_accepts_own_ron_income():
    linked, reason = link_income({"personal_numeric_code": CNP}, income(5000), 0)
    assert linked is not None and reason["code"] == "INCOME_LINKED"


@pytest.mark.parametrize("inc", [income(5000, cnp="2900101123456"), income(5000, currency="EUR"),
                                 {"subject": {"personal_numeric_code": CNP}, "monthly_income": {"currency": "RON", "periods": []}}])
def test_linker_rejects(inc):
    linked, reason = link_income({"personal_numeric_code": CNP}, inc, 0)
    assert linked is None and reason["code"] in ("INCOME_NOT_LINKED", "INCOME_UNUSABLE")
