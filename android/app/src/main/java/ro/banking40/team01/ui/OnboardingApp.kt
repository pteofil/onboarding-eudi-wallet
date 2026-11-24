package ro.banking40.team01.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import ro.banking40.team01.R
import ro.banking40.team01.onboarding.OnboardingViewModel
import ro.banking40.team01.onboarding.Screen

@Composable
fun OnboardingApp(viewModel: OnboardingViewModel) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    val formNotice = stringResource(R.string.form_not_available)

    LaunchedEffect(state.showFormNotice) {
        if (state.showFormNotice) {
            snackbar.showSnackbar(formNotice)
            viewModel.dismissFormNotice()
        }
    }
    BackHandler(enabled = state.screen != Screen.Start) { viewModel.back() }

    Scaffold(snackbarHost = { SnackbarHost(snackbar) }) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
            when (val screen = state.screen) {
                Screen.Start -> StartScreen(
                    onContinue = viewModel::openConsent,
                    onFillForm = viewModel::showFormNotice,
                )
                Screen.Consent -> ConsentScreen(
                    shareIncome = state.shareIncome,
                    onShareIncomeChange = viewModel::setShareIncome,
                    onContinue = viewModel::continueWithWallet,
                )
                Screen.Handoff -> HandoffScreen(onTryAgain = viewModel::tryAgain)
                Screen.Verifying -> VerifyingScreen()
                Screen.Review -> state.review?.let { review ->
                    ReviewScreen(
                        review = review,
                        contact = state.contact,
                        onPhoneChange = viewModel::onPhoneChange,
                        onEmailChange = viewModel::onEmailChange,
                        onSendOtp = viewModel::sendOtp,
                        onOtpChange = viewModel::onOtpChange,
                        onContinue = viewModel::continueFromReview,
                    )
                }
                Screen.Package -> state.review?.let { review ->
                    PackageScreen(
                        decision = review.decision,
                        selected = state.selectedPackage,
                        onSelect = viewModel::selectPackage,
                        onContinue = viewModel::continueFromPackage,
                    )
                }
                Screen.Confirm -> state.selectedPackage?.let { selected ->
                    ConfirmScreen(
                        packageName = selected,
                        contact = state.contact,
                        termsAccepted = state.termsAccepted,
                        signing = state.signing,
                        onTermsChange = viewModel::setTermsAccepted,
                        onSign = viewModel::sign,
                    )
                }
                is Screen.Welcome -> WelcomeScreen(screen.confirmation, onDone = viewModel::reset)
                is Screen.Error -> ErrorScreen(
                    kind = screen.kind,
                    onTryAgain = viewModel::tryAgain,
                    onFillForm = viewModel::showFormNotice,
                    onBackToStart = viewModel::reset,
                )
            }
        }
    }
}
