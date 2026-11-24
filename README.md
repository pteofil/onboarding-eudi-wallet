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
```

Secrets live only in `backend/.env` (local) and Lambda environment variables (cloud). The app never holds them.

## Prepared before kickoff
- Repository skeleton, `.gitignore`
- `backend/verifier_client.py` - HMAC-signed Website API client (checked against the Starter Pack sample)
- `backend/validator.py` + tests - sealed result decryption and validation (Starter Pack page 11)

## Mocked / simplified
- Package thresholds and benefits are illustrative
- No real account opening; the flow ends at a confirmation screen
