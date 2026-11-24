package ro.banking40.team01

import android.content.ActivityNotFoundException
import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.core.net.toUri
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import kotlinx.coroutines.launch
import ro.banking40.team01.onboarding.OnboardingViewModel
import ro.banking40.team01.ui.OnboardingApp
import ro.banking40.team01.ui.theme.VerifiedStartTheme
import ro.banking40.team01.wallet.WalletReturn

class MainActivity : ComponentActivity() {

    private val viewModel: OnboardingViewModel by viewModels { OnboardingViewModel.Factory }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        // On recreation the original intent comes back; a new wallet return arrives via onNewIntent.
        if (savedInstanceState == null) handleWalletReturn(intent)

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.walletLinks.collect(::openWallet)
            }
        }
        setContent {
            VerifiedStartTheme {
                OnboardingApp(viewModel)
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleWalletReturn(intent)
    }

    override fun onResume() {
        super.onResume()
        viewModel.onAppResumed()
    }

    private fun handleWalletReturn(intent: Intent?) {
        if (intent?.action != Intent.ACTION_VIEW) return
        val uri = intent.dataString ?: return
        // The format is confirmed (#response_code=...); the code itself is a one-time secret, so it is masked.
        if (BuildConfig.DEBUG) Log.d(TAG, "Wallet returned: ${WalletReturn.redacted(uri)}")
        if (WalletReturn.isWalletReturn(uri)) viewModel.onWalletReturn(uri)
    }

    private fun openWallet(deepLink: String) {
        try {
            startActivity(Intent(Intent.ACTION_VIEW, deepLink.toUri()))
        } catch (e: ActivityNotFoundException) {
            Log.w(TAG, "No app can open the wallet link")
            viewModel.onWalletNotFound()
        }
    }

    private companion object {
        const val TAG = "VerifiedStart"
    }
}
