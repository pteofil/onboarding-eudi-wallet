package ro.banking40.team01.ui

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
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
        Button(onClick = onContinue, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.start_continue_wallet))
        }
        OutlinedButton(onClick = onFillForm, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.start_fill_form))
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun StartScreenPreview() = VerifiedStartTheme { StartScreen({}, {}) }
