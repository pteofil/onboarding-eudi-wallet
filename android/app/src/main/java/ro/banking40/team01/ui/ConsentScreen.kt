package ro.banking40.team01.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import ro.banking40.team01.R
import ro.banking40.team01.ui.theme.VerifiedStartTheme

/** 2 Consent: what we ask the wallet for, and why. Income is optional and off by default. */
@Composable
fun ConsentScreen(
    shareIncome: Boolean,
    onShareIncomeChange: (Boolean) -> Unit,
    onContinue: () -> Unit,
) {
    StepLayout(
        title = stringResource(R.string.consent_title),
        body = stringResource(R.string.consent_body),
    ) {
        ConsentRow(
            title = stringResource(R.string.consent_identity_title),
            what = stringResource(R.string.consent_identity_what),
            why = stringResource(R.string.consent_identity_why),
            required = true,
        )
        ConsentRow(
            title = stringResource(R.string.consent_address_title),
            what = stringResource(R.string.consent_address_what),
            why = stringResource(R.string.consent_address_why),
            required = true,
        )
        ConsentRow(
            title = stringResource(R.string.consent_income_title),
            what = stringResource(R.string.consent_income_what),
            why = stringResource(R.string.consent_income_why),
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
                    text = stringResource(R.string.consent_income_switch),
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.weight(1f),
                )
                Switch(checked = shareIncome, onCheckedChange = null)
            }
        }
        Text(
            text = stringResource(R.string.consent_nothing_shared_yet),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.secondary,
        )
        Button(onClick = onContinue, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.consent_continue))
        }
    }
}

@Composable
private fun ConsentRow(
    title: String,
    what: String,
    why: String,
    required: Boolean,
    extra: @Composable () -> Unit = {},
) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(title, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                Text(
                    text = stringResource(if (required) R.string.consent_required else R.string.consent_optional),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.secondary,
                )
            }
            Text(what, style = MaterialTheme.typography.bodyMedium)
            Text(
                text = stringResource(R.string.consent_why, why),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.secondary,
            )
            extra()
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun ConsentPreview() = VerifiedStartTheme { ConsentScreen(shareIncome = true, onShareIncomeChange = {}, onContinue = {}) }
