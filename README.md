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
| `POST /applications {shareIncome, returnToApp?}` | `{applicationId, deepLink, status}`; open `deepLink` with ACTION_VIEW. Leave `returnToApp` off unless the app handles the callback (see below) |
| `POST /applications/{id}/claim {responseCode}` | status; only with `returnToApp: true`, when the wallet returns to `ro.banking40.team01://same-device` |
| `GET /applications/{id}/status` | `PENDING` (+`walletStatus`, e.g. `REQUEST_RETRIEVED`, `WAITING_FOR_APP_RETURN`), `VERIFIED`, `DECLINED`, `EXPIRED`, `WALLET_ERROR`, `NOT_VERIFIED`, `UNDER_18`, `MISSING_IDENTITY`, `CONFIRMED` |
| `GET /applications/{id}/review` | verified identity, masked CNP, address, `incomeShared`, package `decision` |
| `POST /applications/{id}/confirm {package?}` | `{package, iban, reference}` (simulated) |

Stub on request: with `ALLOW_STUB=1` on a real backend, `POST /applications {"stub": true}` returns fixed test data straight away, without a wallet. Its review carries `stub: true`, and it's for UI work only. `STUB_VERIFIER=1` makes every request a stub, for local work without Tremend credentials.

Same-device return. Without `returnToApp` the customer approves in the wallet, taps Close and switches back to the app, which keeps polling `status`. This is tested with the real wallet. With `returnToApp: true` the verifier withholds status and result until the app posts the wallet's `response_code`. The wallet only returns to an app with our application ID that is signed with `team-debug.p12`; until that works, the application stays at `WAITING_FOR_APP_RETURN`.

Package rules (`rules.py`), set with env vars: `STARTER_MAX_AGE` (25), `PLUS_MIN_MONTHLY_RON` (5000), `PREMIUM_MIN_MONTHLY_RON` (10000), `TRAVEL_INSURANCE_MAX_AGE` (70), `LIFE_INSURANCE_MAX_AGE` (80). Income is the lowest of the six monthly net amounts. To check a scenario with the real wallet: `python3 try_verification.py onboard-full --decide`.

Deploy (arm64, Python 3.12, handler `handler.lambda_handler`, Function URL):
```
AWS_PROFILE=<profile> FUNCTION_NAME=<lambda name> python3 deploy.py
```
Lambda env vars: `USE_DYNAMO=1`, `TABLE_NAME` (partition key `pk` String, TTL `expiresAt`), the verifier settings
from `.env.example`, or `STUB_VERIFIER=1` while the verifier isn't configured.

The stub verifier's `deepLink` points straight back to the app's callback, so the hand-off can be built without Tremend.
It skips all trust checks and must never be used in the demo.

Secrets live only in `backend/.env` (local) and Lambda environment variables (cloud). The app never holds them.

## Prepared before kickoff
- Repository skeleton, `.gitignore`
- `backend/verifier_client.py` - HMAC-signed Website API client (checked against the Starter Pack sample)
- `backend/validator.py` + tests - sealed result decryption and validation (Starter Pack page 11)

Built during the event: `service.py`, `handler.py`, `store.py`, `local_server.py`, `stub_verifier.py`, `deploy.py`.

## Mocked / simplified
- Package thresholds and benefits are illustrative
- No real account opening; the flow ends at a confirmation screen
