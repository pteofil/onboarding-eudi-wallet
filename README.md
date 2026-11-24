# VerifiedStart

Bank onboarding with the EUDI Wallet: the customer shares verified identity, address and (optionally)
income from the wallet; nothing is typed. Banking 4.0 EUDI Wallet hackathon, Team 01.

## Stack
- `android/` - Kotlin + Jetpack Compose app (`ro.banking40.team01`)
- `backend/` - Python 3.12 AWS Lambda (Function URL) that talks to the Tremend Verifier,
  opens and validates the sealed result, and recommends a package (Simple / Starter / Plus / Premium)
- `infra/` - AWS SAM template

## Backend
```
cd backend
python3 -m pip install -r requirements-dev.txt
cp .env.example .env   # fill in locally, never commit
python3 -m pytest
python3 local_server.py   # http://localhost:8081; without verifier settings it runs the STUB verifier
adb reverse tcp:8081 tcp:8081   # phone -> laptop
```

API (JSON):
| Call | Returns |
|---|---|
| `POST /applications {shareIncome}` | `{applicationId, deepLink, status}`; open `deepLink` with ACTION_VIEW |
| `POST /applications/{id}/claim {responseCode}` | status; call when the wallet returns to `ro.banking40.team01://same-device` |
| `GET /applications/{id}/status` | `PENDING` (+`walletStatus`), `VERIFIED`, `DECLINED`, `EXPIRED`, `WALLET_ERROR`, `NOT_VERIFIED`, `UNDER_18`, `MISSING_IDENTITY`, `CONFIRMED` |
| `GET /applications/{id}/review` | verified identity, masked CNP, address, `incomeShared`, package `decision` |
| `POST /applications/{id}/confirm {package?}` | `{package, iban, reference}` (simulated) |

The stub verifier's `deepLink` points straight back to the app's callback, so the hand-off can be built without Tremend.
It skips all trust checks and must never be used in the demo.

Secrets live only in `backend/.env` (local) and Lambda environment variables (cloud). The app never holds them.

## Prepared before kickoff
- Repository skeleton, `.gitignore`
- `backend/verifier_client.py` - HMAC-signed Website API client (checked against the Starter Pack sample)
- `backend/validator.py` + tests - sealed result decryption and validation (Starter Pack page 11)

Built during the event: `service.py`, `handler.py`, `store.py`, `local_server.py`, `stub_verifier.py`.

## Mocked / simplified
- Package thresholds and benefits are illustrative
- No real account opening; the flow ends at a confirmation screen
