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

Deploy (arm64, Python 3.12, handler `handler.lambda_handler`, Function URL):
```
AWS_PROFILE=<profile> FUNCTION_NAME=<lambda name> python3 deploy.py
```
Lambda env vars: `USE_DYNAMO=1`, `TABLE_NAME` (partition key `pk` String, TTL `expiresAt`), the verifier settings
from `.env.example`, or `STUB_VERIFIER=1` while the verifier isn't configured.

The stub verifier's `deepLink` points straight back to the app's callback, so the hand-off can be built without Tremend.
It skips all trust checks and must never be used in the demo.

Secrets live only in `backend/.env` (local) and Lambda environment variables (cloud). The app never holds them.

## Android
Kotlin + Compose, one Activity, ViewModel + StateFlow, Retrofit/OkHttp + kotlinx.serialization.
Java 17 bytecode (builds on Android Studio's bundled JDK), compileSdk 37, minSdk 29, Gradle 9.8.2, AGP 9.5.1, Kotlin 2.4.21.

1. Copy `android/local.properties.example` to `android/local.properties` (git-ignored) and fill in:
   - `backendBaseUrl` - `http://localhost:8081` (with `adb reverse tcp:8081 tcp:8081`) or the Lambda Function URL
   - `teamDebug.storeFile` - absolute path to `team-debug.p12`, kept **outside** the repo
   - `teamDebug.storePassword`, `teamDebug.keyAlias`, `teamDebug.keyPassword` - from `android-sdk/README.md` in the team ZIP
2. `cd android && ./gradlew :app:installDebug` (unit tests: `./gradlew :app:testDebugUnitTest`).

Without `teamDebug.storeFile` the build falls back to the default debug key and the wallet will not return to the app.

Wallet hand-off: `POST /applications` -> open `deepLink` -> the wallet returns to
`ro.banking40.team01://same-device#response_code=...` -> `POST /claim` -> poll `/status` every 1.5 s.
The app only shows what the backend decides; the wallet's return alone never counts as verified.
Debug builds log the full return URI (tag `VerifiedStart`) because the exact format is not confirmed yet.

## Prepared before kickoff
- Repository skeleton, `.gitignore`
- `backend/verifier_client.py` - HMAC-signed Website API client (checked against the Starter Pack sample)
- `backend/validator.py` + tests - sealed result decryption and validation (Starter Pack page 11)

Built during the event: `service.py`, `handler.py`, `store.py`, `local_server.py`, `stub_verifier.py`, `deploy.py`, `android/`.

## Mocked / simplified
- Package thresholds and benefits are illustrative
- No real account opening; the flow ends at a confirmation screen
