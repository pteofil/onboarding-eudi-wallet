package ro.banking40.team01.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import ro.banking40.team01.R
import ro.banking40.team01.ui.theme.VerifiedStartTheme

/** 1 Become a client. */
@Composable
fun StartScreen(onContinue: () -> Unit, onFillForm: () -> Unit) {
    StepLayout(
        title = stringResource(R.string.start_title),
        body = stringResource(R.string.start_body),
        eyebrow = stringResource(R.string.solution_name),
    ) {
        Option(stringResource(R.string.start_wallet_hint)) {
            Button(onClick = onContinue, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.start_continue_wallet))
            }
        }
        Option(stringResource(R.string.start_manual_hint)) {
            OutlinedButton(onClick = onFillForm, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.start_fill_form))
            }
        }
    }
}

/** A button with a one-line explanation of what that path means for the customer. */
@Composable
private fun Option(hint: String, button: @Composable () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        button()
        Text(
            text = hint,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.secondary,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun StartScreenPreview() = VerifiedStartTheme { StartScreen({}, {}) }
