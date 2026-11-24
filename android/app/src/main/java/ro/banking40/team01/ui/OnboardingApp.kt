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
    BackHandler(enabled = state.screen != Screen.Start) { viewModel.reset() }

    Scaffold(snackbarHost = { SnackbarHost(snackbar) }) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
            when (val screen = state.screen) {
                Screen.Start -> StartScreen(
                    onContinue = { viewModel.continueWithWallet() },
                    onFillForm = viewModel::showFormNotice,
                )
                Screen.Handoff -> HandoffScreen(onTryAgain = viewModel::tryAgain)
                Screen.Verifying -> VerifyingScreen()
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
