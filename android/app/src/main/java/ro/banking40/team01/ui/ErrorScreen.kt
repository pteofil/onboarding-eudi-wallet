package ro.banking40.team01.ui

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import ro.banking40.team01.R
import ro.banking40.team01.onboarding.ErrorKind
import ro.banking40.team01.ui.theme.VerifiedStartTheme

/** One reusable screen for every way the wallet step can end without an account. */
@Composable
fun ErrorScreen(
    kind: ErrorKind,
    onTryAgain: () -> Unit,
    onFillForm: () -> Unit,
    onBackToStart: () -> Unit,
) {
    val (title, body) = kind.texts()
    StepLayout(title = stringResource(title), body = stringResource(body)) {
        if (kind.canRetry) {
            Button(onClick = onTryAgain, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(kind.retryLabel()))
            }
        }
        if (kind.offersForm) {
            OutlinedButton(onClick = onFillForm, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.action_fill_form))
            }
        }
        TextButton(onClick = onBackToStart, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.action_back_to_start))
        }
    }
}

/** A fresh request ("Try again") after the customer's own choice or a timeout; "Retry" after a failure. */
private fun ErrorKind.retryLabel(): Int = when (this) {
    ErrorKind.WALLET_ERROR, ErrorKind.NOT_VERIFIED, ErrorKind.TECHNICAL -> R.string.action_retry
    else -> R.string.action_try_again
}

private fun ErrorKind.texts(): Pair<Int, Int> = when (this) {
    ErrorKind.DECLINED -> R.string.error_declined_title to R.string.error_declined_body
    ErrorKind.EXPIRED -> R.string.error_expired_title to R.string.error_expired_body
    ErrorKind.WALLET_ERROR -> R.string.error_wallet_title to R.string.error_wallet_body
    ErrorKind.NOT_VERIFIED -> R.string.error_not_verified_title to R.string.error_not_verified_body
    ErrorKind.MISSING_IDENTITY -> R.string.error_missing_identity_title to R.string.error_missing_identity_body
    ErrorKind.UNDER_18 -> R.string.error_under_18_title to R.string.error_under_18_body
    ErrorKind.NO_WALLET -> R.string.error_no_wallet_title to R.string.error_no_wallet_body
    ErrorKind.TECHNICAL -> R.string.error_technical_title to R.string.error_technical_body
}

@Preview(showBackground = true)
@Composable
private fun ErrorPreview() = VerifiedStartTheme { ErrorScreen(ErrorKind.DECLINED, {}, {}, {}) }
