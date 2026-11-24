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
import ro.banking40.team01.api.ReviewResponse
import ro.banking40.team01.api.backendErrorCode
import ro.banking40.team01.wallet.WalletReturn
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.minutes

/** Numbers match the journey's screen list; 2, 5, 6 and 7 come after lunch. */
sealed interface Screen {
    data object Landing : Screen // animated intro
    data object Dashboard : Screen // after onboarding
    data object Start : Screen // 1 Become a client
    data object FormDetails : Screen // manual form 1/2 (demo only)
    data object FormAddress : Screen // manual form 2/2 (demo only)
    data object FormSubmitted : Screen
    data object Consent : Screen // 2 Consent
    data object Handoff : Screen // 3 Wallet hand-off
    data object Verifying : Screen // 4 Verifying
    data object Review : Screen // 5 Review your details
    data object Package : Screen // 6 Your package
    data object Confirm : Screen // 7 Confirm
    data class Welcome(val confirmation: ConfirmResponse?) : Screen // 8 Welcome
    data class Error(val kind: ErrorKind) : Screen
}

/** Short messages for actions the demo shows but doesn't implement. */
enum class Notice { NOT_IN_DEMO }

data class UiState(
    val screen: Screen = Screen.Start,
    val notice: Notice? = null,
    val form: ManualForm = ManualForm(),
    val account: Account? = null,
    /** The consent screen's income switch; sent as `shareIncome` in POST /applications. */
    val shareIncome: Boolean = false,
    /** GET /review, loaded once the backend says VERIFIED. */
    val review: ReviewResponse? = null,
    val contact: Contact = Contact(),
    /** Starts as the backend's recommendation; the customer may pick another eligible package. */
    val selectedPackage: String? = null,
    val termsAccepted: Boolean = false,
    /** True while the simulated signature and POST /confirm run. */
    val signing: Boolean = false,
)

class OnboardingViewModel(
    private val saved: SavedStateHandle,
    private val api: OnboardingApi,
) : ViewModel() {

    // Survive process death while the wallet is in the foreground.
    private var applicationId: String?
        get() = saved[KEY_APPLICATION_ID]
        set(value) = saved.set(KEY_APPLICATION_ID, value)
    private var savedShareIncome: Boolean
        get() = saved[KEY_SHARE_INCOME] ?: false
        set(value) = saved.set(KEY_SHARE_INCOME, value)
    private var lastResponseCode: String?
        get() = saved[KEY_LAST_RESPONSE_CODE]
        set(value) = saved.set(KEY_LAST_RESPONSE_CODE, value)

    private val _state = MutableStateFlow(
        UiState(screen = if (applicationId != null) Screen.Handoff else Screen.Landing, shareIncome = savedShareIncome),
    )
    val state: StateFlow<UiState> = _state.asStateFlow()

    private val _walletLinks = Channel<String>(Channel.BUFFERED)

    /** Deep links the Activity opens with ACTION_VIEW. */
    val walletLinks: Flow<String> = _walletLinks.receiveAsFlow()

    private var job: Job? = null

    /** For the dashboard's "opened in 1 min 42 s"; in memory only. */
    private var journeyStartedAt: Long? = null

    fun getStarted() = show(Screen.Start)

    fun openConsent() {
        journeyStartedAt = System.currentTimeMillis()
        show(Screen.Consent)
    }

    /** Welcome -> dashboard; without account details (e.g. after a restart) back to the start. */
    fun openDashboard() = if (_state.value.account != null) show(Screen.Dashboard) else reset()

    fun logout() = reset()

    fun openForm() = show(Screen.FormDetails)

    fun updateForm(form: ManualForm) = _state.update { it.copy(form = form) }

    fun formNext() = show(Screen.FormAddress)

    fun submitForm() = show(Screen.FormSubmitted)

    fun setShareIncome(share: Boolean) = _state.update { it.copy(shareIncome = share) }

    fun continueWithWallet() {
        savedShareIncome = _state.value.shareIncome
        startApplication()
    }

    /** A cancel in the wallet leaves the old request pending until it expires: start a fresh one. */
    fun tryAgain() = startApplication()

    fun reset() {
        job?.cancel()
        applicationId = null
        lastResponseCode = null
        _state.update { UiState(shareIncome = it.shareIncome) }
    }

    /** System back: one step back where that makes sense, otherwise abandon the application. */
    fun back() = when (_state.value.screen) {
        Screen.Consent, Screen.FormDetails, Screen.FormSubmitted -> show(Screen.Start)
        Screen.FormAddress -> show(Screen.FormDetails)
        Screen.Package -> show(Screen.Review)
        Screen.Confirm -> if (!_state.value.signing) show(Screen.Package) else Unit
        else -> reset()
    }

    /** A new number needs a new code. */
    fun onPhoneChange(phone: String) =
        updateContact { if (phone == it.phone) it else it.copy(phone = phone, otpSent = false, otp = "") }

    fun onEmailChange(email: String) = updateContact { it.copy(email = email.trim()) }

    fun sendOtp() = updateContact { it.copy(otpSent = true, otp = Contact.DEMO_OTP) }

    fun onOtpChange(otp: String) =
        updateContact { it.copy(otp = otp.filter(Char::isDigit).take(Contact.OTP_LENGTH)) }

    fun continueFromReview() {
        if (_state.value.contact.complete) show(Screen.Package)
    }

    /** Only packages the backend listed as eligible can be picked. */
    fun selectPackage(name: String) = _state.update {
        if (it.review?.decision?.eligible?.contains(name) == true) it.copy(selectedPackage = name) else it
    }

    fun continueFromPackage() {
        if (_state.value.selectedPackage != null) show(Screen.Confirm)
    }

    fun setTermsAccepted(accepted: Boolean) = _state.update { it.copy(termsAccepted = accepted) }

    /** Signing is simulated (no real SCA); then the backend opens the account. */
    fun sign() {
        val id = applicationId ?: return
        val state = _state.value
        val selected = state.selectedPackage ?: return
        if (!state.termsAccepted || state.signing) return
        job?.cancel()
        _state.update { it.copy(signing = true) }
        job = viewModelScope.launch {
            delay(SIMULATED_SIGNING)
            confirm(id, selected)
        }
    }

    fun showNotice(notice: Notice) = _state.update { it.copy(notice = notice) }

    fun dismissNotice() = _state.update { it.copy(notice = null) }

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
        _state.update { UiState(screen = Screen.Handoff, shareIncome = it.shareIncome) }
        job = viewModelScope.launch {
            try {
                val created = api.create(
                    CreateApplicationRequest(
                        shareIncome = savedShareIncome,
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
            StatusOutcome.Verified -> loadReview(id)
            StatusOutcome.Confirmed -> show(Screen.Welcome(confirmation = null))
            is StatusOutcome.Failed -> show(Screen.Error(outcome.kind))
        }
        Log.i(TAG, "Application $id finished with $status")
        return true
    }

    private suspend fun loadReview(id: String) {
        show(Screen.Verifying)
        try {
            val review = api.review(id)
            _state.update {
                it.copy(
                    review = review,
                    contact = Contact.demoFor(review.identity),
                    selectedPackage = review.decision.recommended,
                    screen = Screen.Review,
                )
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            fail("Review", e)
        }
    }

    private suspend fun confirm(id: String, packageName: String) {
        try {
            val confirmation = api.confirm(id, ConfirmRequest(packageName))
            // The verified details are no longer needed once the account is opened.
            val seconds = journeyStartedAt?.let { (System.currentTimeMillis() - it) / 1000 }
            _state.update {
                val identity = it.review?.identity
                val account = Account(
                    firstName = identity?.givenName?.let(::displayName),
                    fullName = listOfNotNull(identity?.givenName, identity?.familyName).joinToString(" ").ifBlank { null },
                    packageName = confirmation.packageName,
                    iban = confirmation.iban,
                    reference = confirmation.reference,
                    onboardingSeconds = seconds,
                )
                UiState(screen = Screen.Welcome(confirmation), shareIncome = it.shareIncome, account = account)
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            fail("Confirm", e)
        }
    }

    private fun fail(step: String, e: Exception) {
        Log.w(TAG, "$step failed: ${e.describe()}")
        _state.update { it.copy(screen = Screen.Error(ErrorKind.TECHNICAL), signing = false) }
    }

    private fun updateContact(change: (Contact) -> Contact) =
        _state.update { it.copy(contact = change(it.contact)) }

    private fun show(screen: Screen) = _state.update { it.copy(screen = screen) }

    private fun Exception.describe(): String =
        (this as? HttpException)?.let { "HTTP ${it.code()} ${it.backendErrorCode()}" } ?: "${javaClass.simpleName}: ${message?.lineSequence()?.first()}"

    companion object {
        private const val TAG = "VerifiedStart"
        private const val KEY_APPLICATION_ID = "applicationId"
        private const val KEY_SHARE_INCOME = "shareIncome"
        private const val KEY_LAST_RESPONSE_CODE = "lastResponseCode"
        private val POLL_INTERVAL = 1500.milliseconds
        private val POLL_TIMEOUT = 3.minutes
        private val SIMULATED_SIGNING = 1200.milliseconds

        val Factory = viewModelFactory {
            initializer {
                OnboardingViewModel(createSavedStateHandle(), OnboardingApi.create(BuildConfig.BACKEND_BASE_URL))
            }
        }
    }
}
