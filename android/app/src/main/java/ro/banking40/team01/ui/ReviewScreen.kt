package ro.banking40.team01.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.autofill.ContentType
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentType
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import ro.banking40.team01.R
import ro.banking40.team01.api.Decision
import ro.banking40.team01.api.Identity
import ro.banking40.team01.api.ReviewResponse
import ro.banking40.team01.api.TrustCheck
import ro.banking40.team01.api.TrustReport
import ro.banking40.team01.onboarding.Contact
import ro.banking40.team01.ui.theme.VerifiedStartTheme

/** 5 Review your details: verified data is read-only; only phone and email are typed. */
@Composable
fun ReviewScreen(
    review: ReviewResponse,
    contact: Contact,
    onPhoneChange: (String) -> Unit,
    onEmailChange: (String) -> Unit,
    onSendOtp: () -> Unit,
    onOtpChange: (String) -> Unit,
    onContinue: () -> Unit,
) {
    StepLayout(
        title = stringResource(R.string.review_title),
        body = stringResource(R.string.review_body),
    ) {
        if (review.stub) TestDataChip()
        VerifiedDetails(review)
        review.trust?.let { TrustSection(it) }
        ContactDetails(contact, onPhoneChange, onEmailChange, onSendOtp, onOtpChange)
        Button(onClick = onContinue, enabled = contact.complete, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.review_continue))
        }
    }
}

@Composable
private fun VerifiedDetails(review: ReviewResponse) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = stringResource(R.string.review_verified_section),
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.weight(1f),
                )
                VerifiedBadge()
            }
            val name = listOfNotNull(review.identity.givenName, review.identity.familyName).joinToString(" ")
            if (name.isNotBlank()) LabeledValue(stringResource(R.string.review_name), name)
            formatDate(review.identity.birthDate)?.let { LabeledValue(stringResource(R.string.review_birth_date), it) }
            jsonText(review.identity.nationality)?.let { LabeledValue(stringResource(R.string.review_nationality), it) }
            review.personalNumericCode?.let { LabeledValue(stringResource(R.string.review_cnp), it) }
            addressLines(review.address).takeIf { it.isNotEmpty() }?.let {
                LabeledValue(stringResource(R.string.review_address), it.joinToString("\n"))
            }
            LabeledValue(
                label = stringResource(R.string.review_income),
                value = stringResource(if (review.incomeShared) R.string.review_income_shared else R.string.review_income_not_shared),
            )
        }
    }
}

@Composable
private fun TrustSection(trust: TrustReport) {
    var expanded by rememberSaveable { mutableStateOf(false) }
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        TextButton(onClick = { expanded = !expanded }) {
            Text(stringResource(if (expanded) R.string.trust_hide else R.string.trust_show, trust.checks.size))
        }
        if (expanded) {
            trust.note?.let {
                Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
            }
            trust.checks.forEach { TrustLine(it) }
        }
    }
}

@Composable
private fun TrustLine(check: TrustCheck) {
    val (mark, suffix) = when (check.result) {
        "passed" -> "✓" to null
        "failed" -> "✗" to stringResource(R.string.trust_failed)
        "not_shared" -> "–" to stringResource(R.string.trust_not_shared)
        else -> "–" to stringResource(R.string.trust_skipped)
    }
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(mark, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary)
        Text(
            text = listOfNotNull(check.label, suffix, check.detail).joinToString(" · "),
            style = MaterialTheme.typography.bodySmall,
        )
    }
}

@Composable
private fun ContactDetails(
    contact: Contact,
    onPhoneChange: (String) -> Unit,
    onEmailChange: (String) -> Unit,
    onSendOtp: () -> Unit,
    onOtpChange: (String) -> Unit,
) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(stringResource(R.string.review_contact_section), style = MaterialTheme.typography.titleMedium)
            Text(
                stringResource(R.string.review_contact_demo),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.secondary,
            )
            OutlinedTextField(
                value = contact.phone,
                onValueChange = onPhoneChange,
                label = { Text(stringResource(R.string.review_phone)) },
                placeholder = { Text("+40 7xx xxx xxx") },
                singleLine = true,
                isError = contact.phone.isNotEmpty() && !contact.phoneValid,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = contact.email,
                onValueChange = onEmailChange,
                label = { Text(stringResource(R.string.review_email)) },
                singleLine = true,
                isError = contact.email.isNotEmpty() && !contact.emailValid,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                modifier = Modifier.fillMaxWidth(),
            )
            if (!contact.otpSent) {
                OutlinedButton(onClick = onSendOtp, enabled = contact.phoneValid, modifier = Modifier.fillMaxWidth()) {
                    Text(stringResource(R.string.review_send_code))
                }
            } else {
                OutlinedTextField(
                    value = contact.otp,
                    onValueChange = onOtpChange,
                    label = { Text(stringResource(R.string.review_code)) },
                    supportingText = {
                        Text(
                            stringResource(
                                if (contact.phoneConfirmed) R.string.review_code_confirmed else R.string.review_code_hint,
                            ),
                        )
                    },
                    singleLine = true,
                    // A one-time code, not a password: keeps autofill from offering to save it.
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier
                        .fillMaxWidth()
                        .semantics { contentType = ContentType.SmsOtpCode },
                )
            }
        }
    }
}

@Preview(showBackground = true, heightDp = 1400)
@Composable
private fun ReviewPreview() = VerifiedStartTheme {
    ReviewScreen(
        review = ReviewResponse(
            applicationId = "id",
            identity = Identity("ANA", "POPESCU", "1996-03-14"),
            personalNumericCode = "2********3456",
            address = JsonObject(mapOf("formatted" to JsonPrimitive("Str. Exemplu 1, 010101 Bucuresti, Romania"))),
            incomeShared = true,
            decision = Decision(status = "ELIGIBLE", recommended = "Premium"),
            trust = TrustReport(checks = listOf(TrustCheck("signature", "Signed by the verifier", result = "passed"))),
            stub = true,
        ),
        contact = Contact(phone = "+40 712 345 678", email = "ana@example.com", otpSent = true, otp = "123456"),
        onPhoneChange = {}, onEmailChange = {}, onSendOtp = {}, onOtpChange = {}, onContinue = {},
    )
}
