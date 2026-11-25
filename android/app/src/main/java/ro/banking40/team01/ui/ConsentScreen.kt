package ro.banking40.team01.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import ro.banking40.team01.R
import ro.banking40.team01.ui.theme.VerifiedStartTheme

/**
 * 2 Consent: what we ask the wallet for, and why; it must match what the wallet shows on approval.
 * Income is off by default: off asks for ONBOARD_BASIC, on for ONBOARD_FULL (the backend picks by `shareIncome`).
 */
@Composable
fun ConsentScreen(
    shareIncome: Boolean,
    onShareIncomeChange: (Boolean) -> Unit,
    onContinue: () -> Unit,
) {
    StepLayout(title = stringResource(R.string.consent_header)) {
        ConsentRow(
            title = stringResource(R.string.consent_identity_title),
            why = stringResource(R.string.consent_identity_body),
            required = true,
        )
        ConsentRow(
            title = stringResource(R.string.consent_address_title),
            why = stringResource(R.string.consent_address_body),
            required = true,
        )
        ConsentRow(
            title = stringResource(R.string.consent_income_title),
            why = stringResource(R.string.consent_income_body),
            required = false,
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .toggleable(value = shareIncome, role = Role.Switch, onValueChange = onShareIncomeChange),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text(
                    text = stringResource(R.string.consent_income_toggle),
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.weight(1f),
                )
                Switch(checked = shareIncome, onCheckedChange = null)
            }
        }
        KycNotice()
        Text(
            text = stringResource(R.string.consent_footer),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.secondary,
        )
        Button(onClick = onContinue, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.consent_continue))
        }
    }
}

/** Tells the customer, before the wallet opens, that the shared identity and address also serve KYC. */
@Composable
private fun KycNotice() {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(
                text = stringResource(R.string.consent_kyc_label),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary,
            )
            Text(stringResource(R.string.consent_kyc_title), style = MaterialTheme.typography.titleMedium)
            Text(
                stringResource(R.string.consent_kyc_body),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
            )
        }
    }
}

@Composable
private fun ConsentRow(
    title: String,
    why: String,
    required: Boolean,
    extra: @Composable () -> Unit = {},
) {
    var showWhy by rememberSaveable(title) { mutableStateOf(false) }
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(
                text = stringResource(if (required) R.string.consent_required else R.string.consent_optional),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.secondary,
            )
            Text(title, style = MaterialTheme.typography.titleMedium)
            TextButton(onClick = { showWhy = !showWhy }, contentPadding = PaddingValues(0.dp)) {
                Text(stringResource(R.string.consent_why_link))
            }
            if (showWhy) {
                Text(why, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.secondary)
            }
            extra()
        }
    }
}

@Preview(showBackground = true, heightDp = 1200)
@Composable
private fun ConsentPreview() = VerifiedStartTheme { ConsentScreen(shareIncome = false, onShareIncomeChange = {}, onContinue = {}) }
