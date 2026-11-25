package ro.banking40.team01.ui

import androidx.activity.compose.BackHandler
import androidx.activity.compose.LocalActivity
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.core.view.WindowCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import ro.banking40.team01.R
import ro.banking40.team01.onboarding.Notice
import ro.banking40.team01.onboarding.OnboardingViewModel
import ro.banking40.team01.onboarding.Screen

@Composable
fun OnboardingApp(viewModel: OnboardingViewModel) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    val noticeText = when (state.notice) {
        Notice.NOT_IN_DEMO -> stringResource(R.string.not_in_demo)
        null -> null
    }

    LaunchedEffect(state.notice) {
        if (noticeText != null) {
            snackbar.showSnackbar(noticeText)
            viewModel.dismissNotice()
        }
    }
    // Full-bleed screens draw their own gradient behind the status bar, with light icons on it.
    val fullBleed = state.screen == Screen.Landing || state.screen == Screen.Dashboard
    val view = LocalView.current
    val window = LocalActivity.current?.window
    LaunchedEffect(fullBleed) {
        window?.let { WindowCompat.getInsetsController(it, view).isAppearanceLightStatusBars = !fullBleed }
    }
    val backLeavesApp = state.screen == Screen.Start || state.screen == Screen.Landing || state.screen == Screen.Dashboard
    BackHandler(enabled = !backLeavesApp) { viewModel.back() }

    Scaffold(snackbarHost = { SnackbarHost(snackbar) }) { padding ->
        Box(Modifier.fillMaxSize().padding(if (fullBleed) PaddingValues() else padding)) {
            when (val screen = state.screen) {
                Screen.Landing -> LandingScreen(onGetStarted = viewModel::getStarted)
                Screen.Start -> StartScreen(
                    onContinue = viewModel::openConsent,
                    onFillForm = viewModel::openForm,
                )
                Screen.FormDetails -> FormDetailsScreen(state.form, viewModel::updateForm, viewModel::formNext)
                Screen.FormAddress -> FormAddressScreen(state.form, viewModel::updateForm, viewModel::submitForm)
                Screen.FormSubmitted -> FormSubmittedScreen(
                    onUseWallet = viewModel::openConsent,
                    onBackToStart = viewModel::reset,
                )
                Screen.Consent -> ConsentScreen(
                    shareIncome = state.shareIncome,
                    onShareIncomeChange = viewModel::setShareIncome,
                    onContinue = viewModel::continueWithWallet,
                )
                Screen.Handoff -> HandoffScreen(
                    incomeRequested = state.shareIncome,
                    onTryAgain = viewModel::tryAgain,
                    onContinueWithoutIncome = viewModel::continueWithoutIncome,
                )
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
                Screen.Kyc -> state.review?.let { review ->
                    KycScreen(
                        identity = review.identity,
                        maskedCnp = review.personalNumericCode,
                        address = review.address,
                        incomeVerified = review.incomeShared,
                        declarations = state.kyc,
                        onChange = viewModel::updateKyc,
                        onContinue = viewModel::continueFromKyc,
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
                is Screen.Welcome -> WelcomeScreen(screen.confirmation, onDone = viewModel::openDashboard)
                Screen.Dashboard -> state.account?.let { account ->
                    DashboardScreen(
                        account = account,
                        onLogout = viewModel::logout,
                        onNotInDemo = { viewModel.showNotice(Notice.NOT_IN_DEMO) },
                    )
                }
                is Screen.Error -> ErrorScreen(
                    kind = screen.kind,
                    onTryAgain = viewModel::tryAgain,
                    onMinors = { viewModel.showNotice(Notice.NOT_IN_DEMO) },
                    onHelp = { viewModel.showNotice(Notice.NOT_IN_DEMO) },
                )
            }
        }
    }
}
