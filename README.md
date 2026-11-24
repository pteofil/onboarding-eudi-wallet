# Verified Onboarding: Flash team

Bank onboarding with the EUDI Wallet for Flash Bank, a fictional bank: the customer shares verified identity, address
and (optionally) income from the wallet; nothing is typed. Banking 4.0 EUDI Wallet hackathon, Team 01 (Flash team).

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
| `POST /applications {shareIncome, returnToApp?}` | `{applicationId, deepLink, status}`; open `deepLink` with ACTION_VIEW. The app sends `returnToApp: true` (see below) |
| `POST /applications/{id}/claim {responseCode}` | status; only with `returnToApp: true`, when the wallet returns to `ro.banking40.team01://same-device` |
| `GET /applications/{id}/status` | `PENDING` (+`walletStatus`, e.g. `REQUEST_RETRIEVED`, `WAITING_FOR_APP_RETURN`), `VERIFIED`, `DECLINED`, `EXPIRED`, `WALLET_ERROR`, `NOT_VERIFIED`, `UNDER_18`, `MISSING_IDENTITY`, `CONFIRMED` |
| `GET /applications/{id}/review` | verified identity (name, birth date, `nationality` when the profile asks for it), masked CNP, address, `incomeShared`, package `decision`, `trust` (see below) |
| `POST /applications/{id}/confirm {package?}` | `{package, iban, reference}` (simulated) |

Stub on request: with `ALLOW_STUB=1` on a real backend, `POST /applications {"stub": true}` returns fixed test data straight away, without a wallet. Its review carries `stub: true`, and it's for UI work only. `STUB_VERIFIER=1` makes every request a stub, for local work without Tremend credentials.

Same-device return. Without `returnToApp` the customer approves in the wallet, taps Close and switches back to the app, which keeps polling `status`. This is tested with the real wallet. With `returnToApp: true` the verifier withholds status and result until the app posts the wallet's `response_code`. The wallet only returns to an app with our application ID, signed with `team-debug.p12`, that has an activity named `eu.europa.ec.euidi.verifier.presentation.ui.SameDeviceCallbackActivity` (the verifier SDK's class). Our app ships a same-named activity that forwards the URI to `MainActivity`; tested with the real wallet. Without the claim the application stays at `WAITING_FOR_APP_RETURN`.

Trust report (`trust.py`), in `review.trust`: `checkedAt`, the credential types shared, and `checks[]` of `{id, label, by, result}`.
`by` is `backend` (our checks on the sealed result: decryption, verifier signature, issuer and audience, freshness,
transaction binding, one-time use, outcome, identity and address present) or `verifier` (the wallet presentation, checked
by the Tremend verifier before it signs). `result` is `passed`, `failed` and `not_shared` (income only), or `skipped` (stub, with a `note`).
A review exists only when every backend check passed, so only the income line can fail. No claim values in it.

Package rules (`rules.py`), set with env vars: `STARTER_MAX_AGE` (25), `PLUS_MIN_MONTHLY_RON` (5000), `PREMIUM_MIN_MONTHLY_RON` (10000), `TRAVEL_INSURANCE_MAX_AGE` (70), `LIFE_INSURANCE_MAX_AGE` (80). Income is the lowest of the six monthly net amounts.
`decision.reasons[]`, `notEligible[]`, `insurance.*` and `assumptions[]` carry a `code` and an English `text`, plus `age` (int) or
`amount` (e.g. `"16,000"`) when the text has a placeholder. The app shows its own string `reason_<code in lower case>` from
`res/values/strings.xml` (e.g. `INCOME_PREMIUM` -> `reason_income_premium`, `TRAVEL_INS_AGE` -> `reason_travel_ins_age`;
assumptions -> `assumption_<code>`). Codes: `AGE_ADULT`, `AGE_UNDER_18`, `STARTER_AGE`, `STARTER_TOO_OLD`, `NEEDS_PLUS`,
`NEEDS_PREMIUM`, `INCOME_PLUS`, `INCOME_PREMIUM`, `LOWEST_MONTH`, `INCOME_NOT_SHARED`, `INCOME_NOT_LINKED`,
`INCOME_UNUSABLE`, `TRAVEL_INS_OK`, `TRAVEL_INS_AGE`, `LIFE_INS_OK`, `LIFE_INS_AGE`; assumptions `FICTIONAL`, `INCOME`, `INSURANCE`. To check a scenario with the real wallet: `python3 try_verification.py onboard-full --decide`.

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
   - `backendBaseUrl` - defaults to the hosted Lambda `https://okrzzubfh3hlh4wgjc5vbjc5ym0chayk.lambda-url.eu-central-1.on.aws`;
     use `http://localhost:8081` (with `adb reverse tcp:8081 tcp:8081`) for a local backend
   - `teamDebug.storeFile` - absolute path to `team-debug.p12`, kept **outside** the repo
   - `teamDebug.storePassword`, `teamDebug.keyAlias`, `teamDebug.keyPassword` - from `android-sdk/README.md` in the team ZIP
   - optional `returnToApp` (default `true`) and `stub` (default `false`, backend needs `ALLOW_STUB=1`)
2. `cd android && ./gradlew :app:installDebug` (unit tests: `./gradlew :app:testDebugUnitTest`).

Without `teamDebug.storeFile` the build falls back to the default debug key and the wallet will not return to the app.

Wallet hand-off: `POST /applications` -> open `deepLink` -> the wallet returns to
`ro.banking40.team01://same-device#response_code=...` -> `POST /claim` -> poll `/status` every 1.5 s.
The app only shows what the backend decides; the wallet's return alone never counts as verified.
Debug builds log the full return URI (tag `VerifiedStart`).

The event wallet only returns to the verifier SDK's callback class inside the team package
(`eu.europa.ec.euidi.verifier.presentation.ui.SameDeviceCallbackActivity`); it does nothing if that class is missing.
We don't use the SDK, so the app ships a same-named activity that forwards the URI to `MainActivity`.
With `returnToApp=false` the user taps Close in the wallet and switches back; the app polls on resume.

## Prepared before kickoff
- Repository skeleton, `.gitignore`
- `backend/verifier_client.py` - HMAC-signed Website API client (checked against the Starter Pack sample)
- `backend/validator.py` + tests - sealed result decryption and validation (Starter Pack page 11)

Built during the event: `service.py`, `handler.py`, `store.py`, `local_server.py`, `stub_verifier.py`, `deploy.py`, `android/`.

## Mocked / simplified
- Package thresholds and benefits are illustrative
- No real account opening; the flow ends at a confirmation screen
