package ro.banking40.team01.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import ro.banking40.team01.R
import ro.banking40.team01.onboarding.Contact
import ro.banking40.team01.ui.theme.VerifiedStartTheme

/** 7 Confirm: accept the terms, then a simulated signature; the backend opens the account. */
@Composable
fun ConfirmScreen(
    packageName: String,
    contact: Contact,
    termsAccepted: Boolean,
    signing: Boolean,
    onTermsChange: (Boolean) -> Unit,
    onSign: () -> Unit,
) {
    StepLayout(title = stringResource(R.string.confirm_title), body = stringResource(R.string.confirm_body)) {
        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                LabeledValue(stringResource(R.string.confirm_package), packageLabel(packageName))
                LabeledValue(stringResource(R.string.confirm_phone), contact.phone)
                LabeledValue(stringResource(R.string.confirm_email), contact.email)
            }
        }
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .toggleable(value = termsAccepted, enabled = !signing, role = Role.Checkbox, onValueChange = onTermsChange),
        ) {
            Checkbox(checked = termsAccepted, onCheckedChange = null, enabled = !signing)
            Text(
                stringResource(R.string.confirm_terms),
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(start = 12.dp),
            )
        }
        Button(onClick = onSign, enabled = termsAccepted && !signing, modifier = Modifier.fillMaxWidth()) {
            if (signing) {
                CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
                Text(stringResource(R.string.confirm_signing), modifier = Modifier.padding(start = 8.dp))
            } else {
                Text(stringResource(R.string.confirm_sign))
            }
        }
        Text(
            stringResource(R.string.confirm_sign_note),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.secondary,
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun ConfirmPreview() = VerifiedStartTheme {
    ConfirmScreen(
        packageName = "Premium",
        contact = Contact(phone = "+40 712 345 678", email = "ana@example.com"),
        termsAccepted = true,
        signing = false,
        onTermsChange = {},
        onSign = {},
    )
}
