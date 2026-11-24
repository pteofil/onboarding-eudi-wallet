package ro.banking40.team01.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.TextButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import ro.banking40.team01.R
import ro.banking40.team01.ui.theme.VerifiedStartTheme

/**
 * 3 Wallet hand-off. Try again is always offered: a cancel in the wallet never calls back.
 * With income requested, the wallet also stays silent when the income document has expired, so the customer
 * can drop income and continue with identity and address only.
 */
@Composable
fun HandoffScreen(incomeRequested: Boolean, onTryAgain: () -> Unit, onContinueWithoutIncome: () -> Unit) {
    StepLayout(
        title = stringResource(R.string.handoff_title),
        body = stringResource(R.string.handoff_body),
    ) {
        Spinner()
        Text(stringResource(R.string.handoff_not_opened), style = MaterialTheme.typography.bodyMedium)
        OutlinedButton(onClick = onTryAgain, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.btn_try_again))
        }
        if (incomeRequested) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                TextButton(onClick = onContinueWithoutIncome, modifier = Modifier.fillMaxWidth()) {
                    Text(stringResource(R.string.btn_continue_without_income))
                }
                Text(
                    text = stringResource(R.string.handoff_income_hint),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.secondary,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}

/** 4 Verifying: waits for the backend's verdict. The wallet's return alone proves nothing. */
@Composable
fun VerifyingScreen() {
    StepLayout(
        title = stringResource(R.string.verifying_title),
        body = stringResource(R.string.verifying_body),
    ) {
        Spinner()
    }
}

@Composable
private fun Spinner() {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
        CircularProgressIndicator()
    }
}

@Preview(showBackground = true)
@Composable
private fun HandoffPreview() = VerifiedStartTheme { HandoffScreen(incomeRequested = true, {}, {}) }
