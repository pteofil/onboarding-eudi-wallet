"""Manual end-to-end check with a real wallet (two devices, QR on the laptop).

    python3 try_verification.py onboard-basic

Starts a verification, opens the QR in the browser, polls, then collects, decrypts and checks
the sealed result exactly like the Lambda does. Prints only the outcome and claim names.
Use the sandbox test customers only.
"""
import base64
import json
import os
import sys
import tempfile
import time
import webbrowser
from pathlib import Path

from validator import load_private_key, open_result
from verifier_client import VerifierClient

for line in Path(__file__).with_name(".env").read_text().splitlines():
    if "=" in line and not line.startswith("#"):
        key, value = line.split("=", 1)
        os.environ.setdefault(key, value)

profile = sys.argv[1] if len(sys.argv) > 1 else os.environ["PROFILE_BASIC"]
client = VerifierClient(os.environ["VERIFIER_BASE_URL"], os.environ["VERIFIER_WEBSITE_KEY"],
                        os.environ["VERIFIER_WEBSITE_SECRET"])
started = client.start(profile)
tx = started["transactionId"]
qr = Path(tempfile.gettempdir()) / f"verifiedstart-{tx}.svg"
qr.write_text(started["qrSvg"])
webbrowser.open(qr.as_uri())
print(f"tx {tx}: scan the QR with the wallet (expires {started.get('expiresAt')})")

status = {}
while True:
    status = client.status(tx)
    print("  status:", status["status"])
    if status["status"] in {"COMPLETED", "DECLINED", "EXPIRED", "WALLET_ERROR", "APPROVED", "REJECTED"}:
        break
    time.sleep(2)
qr.unlink(missing_ok=True)
if status["status"] != "COMPLETED":
    sys.exit(f"finished without a sealed result: {json.dumps(status)}")

payload = open_result(client.collect(tx), load_private_key(base64.b64decode(os.environ["RESULT_PRIVATE_KEY_B64"])),
                      client.jwks(), os.environ["VERIFIER_BASE_URL"], os.environ["VERIFIER_AUDIENCE"], tx)
client.acknowledge(tx)
print("trust checks passed; outcome:", json.dumps(payload.get("outcome")))


def shape(value):
    """Field names and types only, never values."""
    if isinstance(value, dict):
        return {key: shape(item) for key, item in value.items()}
    if isinstance(value, list):
        return [shape(value[0]), f"x{len(value)}"] if value else []
    return type(value).__name__


for credential in payload.get("credentials", []):
    print(f"  {credential.get('type')}: {json.dumps(shape(credential.get('claims') or {}))}")
