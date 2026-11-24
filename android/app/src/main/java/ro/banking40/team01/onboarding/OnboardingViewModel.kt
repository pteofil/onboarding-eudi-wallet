package ro.banking40.team01.onboarding

import android.util.Log
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import retrofit2.HttpException
import ro.banking40.team01.BuildConfig
import ro.banking40.team01.api.ClaimRequest
import ro.banking40.team01.api.ConfirmRequest
import ro.banking40.team01.api.ConfirmResponse
import ro.banking40.team01.api.CreateApplicationRequest
import ro.banking40.team01.api.OnboardingApi
import ro.banking40.team01.api.backendErrorCode
import ro.banking40.team01.wallet.WalletReturn
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.minutes

/** Numbers match the journey's screen list; 2, 5, 6 and 7 come after lunch. */
sealed interface Screen {
    data object Start : Screen // 1 Become a client
    data object Handoff : Screen // 3 Wallet hand-off
    data object Verifying : Screen // 4 Verifying
    data class Welcome(val confirmation: ConfirmResponse?) : Screen // 8 Welcome
    data class Error(val kind: ErrorKind) : Screen
}

data class UiState(
    val screen: Screen = Screen.Start,
    val showFormNotice: Boolean = false,
)

class OnboardingViewModel(
    private val saved: SavedStateHandle,
    private val api: OnboardingApi,
) : ViewModel() {

    // Survive process death while the wallet is in the foreground.
    private var applicationId: String?
        get() = saved[KEY_APPLICATION_ID]
        set(value) = saved.set(KEY_APPLICATION_ID, value)
    private var shareIncome: Boolean
        get() = saved[KEY_SHARE_INCOME] ?: false
        set(value) = saved.set(KEY_SHARE_INCOME, value)
    private var lastResponseCode: String?
        get() = saved[KEY_LAST_RESPONSE_CODE]
        set(value) = saved.set(KEY_LAST_RESPONSE_CODE, value)

    private val _state = MutableStateFlow(
        UiState(screen = if (applicationId != null) Screen.Handoff else Screen.Start),
    )
    val state: StateFlow<UiState> = _state.asStateFlow()

    private val _walletLinks = Channel<String>(Channel.BUFFERED)

    /** Deep links the Activity opens with ACTION_VIEW. */
    val walletLinks: Flow<String> = _walletLinks.receiveAsFlow()

    private var job: Job? = null

    fun continueWithWallet(shareIncome: Boolean = false) {
        this.shareIncome = shareIncome
        startApplication()
    }

    /** A cancel in the wallet leaves the old request pending until it expires: start a fresh one. */
    fun tryAgain() = startApplication()

    fun reset() {
        job?.cancel()
        applicationId = null
        lastResponseCode = null
        show(Screen.Start)
    }

    fun showFormNotice() = _state.update { it.copy(showFormNotice = true) }

    fun dismissFormNotice() = _state.update { it.copy(showFormNotice = false) }

    fun onWalletNotFound() = show(Screen.Error(ErrorKind.NO_WALLET))

    fun onWalletReturn(uri: String) {
        val id = applicationId
        if (id == null) {
            Log.w(TAG, "Wallet returned but there is no open application; ignoring")
            return
        }
        val code = WalletReturn.responseCode(uri)
        if (code != null && code == lastResponseCode) return // same return delivered twice
        job?.cancel()
        if (code == null) {
            // Returned without a code (e.g. cancelled): the status decides, Try again stays visible.
            Log.w(TAG, "Wallet returned without response_code for application $id")
            show(Screen.Handoff)
            job = viewModelScope.launch { poll(id) }
            return
        }
        lastResponseCode = code
        show(Screen.Verifying)
        job = viewModelScope.launch {
            try {
                if (handle(id, api.claim(id, ClaimRequest(code)).status)) return@launch
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                // Could be a late return from an older request: polling decides, not the claim.
                Log.w(TAG, "Claim failed for application $id: ${e.describe()}")
            }
            poll(id)
        }
    }

    /** Called from Activity.onResume: also catches a return from the wallet without a callback. */
    fun onAppResumed() {
        val id = applicationId ?: return
        val screen = _state.value.screen
        if (job?.isActive == true || (screen != Screen.Handoff && screen != Screen.Verifying)) return
        job = viewModelScope.launch { poll(id) }
    }

    private fun startApplication() {
        job?.cancel()
        applicationId = null
        lastResponseCode = null
        show(Screen.Handoff)
        job = viewModelScope.launch {
            try {
                val created = api.create(
                    CreateApplicationRequest(
                        shareIncome = shareIncome,
                        returnToApp = BuildConfig.RETURN_TO_APP,
                        stub = BuildConfig.USE_STUB_VERIFIER,
                    ),
                )
                applicationId = created.applicationId
                Log.i(TAG, "Application ${created.applicationId} created")
                _walletLinks.send(created.deepLink)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                fail("Create application", e)
            }
        }
    }

    private suspend fun poll(id: String) {
        val finished = withTimeoutOrNull(POLL_TIMEOUT) {
            while (!pollOnce(id)) delay(POLL_INTERVAL)
        }
        if (finished == null) show(Screen.Error(ErrorKind.EXPIRED))
    }

    /** Returns true when polling should stop. */
    private suspend fun pollOnce(id: String): Boolean = try {
        handle(id, api.status(id).status)
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        if (e is HttpException && e.code() == 404) {
            fail("Status", e) // the backend no longer knows this application
            true
        } else {
            Log.w(TAG, "Status failed for application $id: ${e.describe()}") // transient: retry
            false
        }
    }

    /** Applies a backend status. Returns true when polling should stop. */
    private suspend fun handle(id: String, status: String): Boolean {
        when (val outcome = outcomeOf(status)) {
            StatusOutcome.KeepPolling -> return false
            // Temporary 4 -> 8 shortcut until Review/Package/Confirm (5-7) exist.
            StatusOutcome.Verified -> confirmRecommended(id)
            StatusOutcome.Confirmed -> show(Screen.Welcome(confirmation = null))
            is StatusOutcome.Failed -> show(Screen.Error(outcome.kind))
        }
        Log.i(TAG, "Application $id finished with $status")
        return true
    }

    private suspend fun confirmRecommended(id: String) {
        try {
            show(Screen.Welcome(api.confirm(id, ConfirmRequest())))
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            fail("Confirm", e)
        }
    }

    private fun fail(step: String, e: Exception) {
        Log.w(TAG, "$step failed: ${e.describe()}")
        show(Screen.Error(ErrorKind.TECHNICAL))
    }

    private fun show(screen: Screen) = _state.update { it.copy(screen = screen) }

    private fun Exception.describe(): String =
        (this as? HttpException)?.let { "HTTP ${it.code()} ${it.backendErrorCode()}" } ?: javaClass.simpleName

    companion object {
        private const val TAG = "VerifiedStart"
        private const val KEY_APPLICATION_ID = "applicationId"
        private const val KEY_SHARE_INCOME = "shareIncome"
        private const val KEY_LAST_RESPONSE_CODE = "lastResponseCode"
        private val POLL_INTERVAL = 1500.milliseconds
        private val POLL_TIMEOUT = 3.minutes

        val Factory = viewModelFactory {
            initializer {
                OnboardingViewModel(createSavedStateHandle(), OnboardingApi.create(BuildConfig.BACKEND_BASE_URL))
            }
        }
    }
}
