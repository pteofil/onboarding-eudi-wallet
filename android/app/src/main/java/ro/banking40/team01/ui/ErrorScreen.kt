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
import ro.banking40.team01.onboarding.ErrorKind
import ro.banking40.team01.ui.theme.VerifiedStartTheme

/** One reusable screen for every way the wallet step can end without an account: Try again and Help only. */
@Composable
fun ErrorScreen(
    kind: ErrorKind,
    onTryAgain: () -> Unit,
    onMinors: () -> Unit,
    onHelp: () -> Unit,
) {
    val (title, body) = kind.texts()
    StepLayout(title = stringResource(title), body = stringResource(body)) {
        if (kind.canRetry) {
            Button(onClick = onTryAgain, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.btn_try_again))
            }
        }
        if (kind == ErrorKind.UNDER_18) {
            Button(onClick = onMinors, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.btn_minors))
            }
        }
        OutlinedButton(onClick = onHelp, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.btn_help))
        }
    }
}

private fun ErrorKind.texts(): Pair<Int, Int> = when (this) {
    ErrorKind.DECLINED -> R.string.err_declined_title to R.string.err_declined_body
    ErrorKind.EXPIRED -> R.string.err_expired_title to R.string.err_expired_body
    ErrorKind.WALLET_ERROR -> R.string.err_wallet_title to R.string.err_wallet_body
    ErrorKind.NOT_VERIFIED -> R.string.err_not_verified_title to R.string.err_not_verified_body
    ErrorKind.MISSING_IDENTITY -> R.string.err_withheld_title to R.string.err_withheld_body
    ErrorKind.UNDER_18 -> R.string.err_under18_title to R.string.err_under18_body
    ErrorKind.NO_WALLET -> R.string.err_no_wallet_title to R.string.err_no_wallet_body
    ErrorKind.TECHNICAL -> R.string.err_technical_title to R.string.err_technical_body
}

@Preview(showBackground = true)
@Composable
private fun ErrorPreview() = VerifiedStartTheme { ErrorScreen(ErrorKind.UNDER_18, {}, {}, {}) }
