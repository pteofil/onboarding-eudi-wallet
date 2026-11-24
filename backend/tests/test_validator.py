import json
import time

import pytest
from jwcrypto import jwe, jwk, jws

from validator import ResultRejected, open_result

ISSUER = "https://verifier.example"
AUDIENCE = "org-123"
TX = "tx-1"
NOW = 1_800_000_000


@pytest.fixture(scope="module")
def keys():
    verifier_signing = jwk.JWK.generate(kty="EC", crv="P-256", kid="verifier-1")
    bank_result_key = jwk.JWK.generate(kty="EC", crv="P-256", kid="bank-1")
    jwks = {"keys": [json.loads(verifier_signing.export_public())]}
    return verifier_signing, bank_result_key, jwks


def payload(**overrides):
    body = {
        "iss": ISSUER, "aud": AUDIENCE, "iat": NOW - 5, "exp": NOW + 300, "jti": "jti-1",
        "schema": "eudi-verification-result/v1", "transactionId": TX,
        "outcome": {"status": "SATISFIED"},
    }
    body.update(overrides)
    return body


def seal(body, signing_key, result_key, typ="eudi-result+jwt", alg="ECDH-ES+A256KW", enc="A256GCM"):
    signed = jws.JWS(json.dumps(body).encode())
    signed.add_signature(signing_key, alg="ES256",
                         protected={"alg": "ES256", "typ": typ, "kid": signing_key["kid"]})
    token = jwe.JWE(signed.serialize(compact=True).encode(),
                    protected={"alg": alg, "enc": enc, "kid": result_key["kid"]})
    token.add_recipient(result_key)
    return token.serialize(compact=True)


def open_(sealed, keys, **kwargs):
    _, result_key, jwks = keys
    args = dict(issuer=ISSUER, audience=AUDIENCE, expected_transaction_id=TX, now=NOW)
    args.update(kwargs)
    return open_result(sealed, result_key, jwks, **args)


def test_valid_result_is_opened(keys):
    signing, result_key, _ = keys
    assert open_(seal(payload(), signing, result_key), keys)["outcome"]["status"] == "SATISFIED"


@pytest.mark.parametrize("overrides, reason", [
    ({"iss": "https://evil.example"}, "wrong_issuer"),
    ({"aud": "other-org"}, "wrong_audience"),
    ({"exp": NOW - 61}, "expired"),
    ({"iat": NOW + 120}, "issued_in_future"),
    ({"schema": "something/v2"}, "wrong_schema"),
    ({"transactionId": "tx-other"}, "wrong_transaction"),
    ({"jti": ""}, "invalid_jti"),
])
def test_rejects_wrong_claims(keys, overrides, reason):
    signing, result_key, _ = keys
    with pytest.raises(ResultRejected) as error:
        open_(seal(payload(**overrides), signing, result_key), keys)
    assert error.value.reason == reason


def test_rejects_missing_jti(keys):
    signing, result_key, _ = keys
    body = payload()
    del body["jti"]
    with pytest.raises(ResultRejected, match="missing_jti"):
        open_(seal(body, signing, result_key), keys)


def test_rejects_result_signed_by_untrusted_key(keys):
    _, result_key, _ = keys
    impostor = jwk.JWK.generate(kty="EC", crv="P-256", kid="verifier-1")
    with pytest.raises(ResultRejected, match="bad_signature"):
        open_(seal(payload(), impostor, result_key), keys)


def test_rejects_wrong_typ(keys):
    signing, result_key, _ = keys
    with pytest.raises(ResultRejected, match="unexpected_typ"):
        open_(seal(payload(), signing, result_key, typ="JWT"), keys)


def test_rejects_result_sealed_for_another_key(keys):
    signing, _, _ = keys
    other = jwk.JWK.generate(kty="EC", crv="P-256", kid="bank-1")
    with pytest.raises(ResultRejected, match="decryption_failed"):
        open_(seal(payload(), signing, other), keys)


def test_rejects_disallowed_content_encryption(keys):
    signing, result_key, _ = keys
    with pytest.raises(ResultRejected, match="jwe_algorithm_not_allowed"):
        open_(seal(payload(), signing, result_key, enc="A128GCM"), keys)


def test_rejects_garbage(keys):
    with pytest.raises(ResultRejected, match="malformed_jwe"):
        open_("not-a-jwe", keys)
