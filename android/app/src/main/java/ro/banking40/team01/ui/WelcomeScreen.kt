package ro.banking40.team01.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import ro.banking40.team01.R
import ro.banking40.team01.api.ConfirmResponse
import ro.banking40.team01.ui.theme.VerifiedStartTheme

/** 8 Welcome. `confirmation` is null when the app only learned the CONFIRMED status (e.g. after a restart). */
@Composable
fun WelcomeScreen(confirmation: ConfirmResponse?, onDone: () -> Unit) {
    StepLayout(
        title = stringResource(R.string.welcome_title, stringResource(R.string.bank_name)),
        body = stringResource(if (confirmation != null) R.string.welcome_body else R.string.welcome_already_confirmed),
    ) {
        if (confirmation != null) {
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Detail(stringResource(R.string.welcome_package), confirmation.packageName)
                    Detail(stringResource(R.string.welcome_iban), confirmation.iban)
                    Detail(stringResource(R.string.welcome_reference), confirmation.reference)
                }
            }
        }
        Button(onClick = onDone, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.welcome_done))
        }
    }
}

@Composable
private fun Detail(label: String, value: String) {
    Column {
        Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.secondary)
        Text(value, style = MaterialTheme.typography.titleMedium)
    }
}

@Preview(showBackground = true)
@Composable
private fun WelcomePreview() = VerifiedStartTheme {
    WelcomeScreen(ConfirmResponse("id", "CONFIRMED", "Simple", "RO12DEMO0000000000000000", "VS-1234ABCD")) {}
}
