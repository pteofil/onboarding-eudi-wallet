package ro.banking40.team01.onboarding

/**
 * Everything that ends the wallet step without an account. One reusable error screen per kind, with only
 * "Try again" (when [canRetry]) and "Help". Withheld income is not here: the backend still recommends Simple or Starter.
 */
enum class ErrorKind(val canRetry: Boolean) {
    DECLINED(canRetry = true),
    EXPIRED(canRetry = true),
    WALLET_ERROR(canRetry = true),
    NOT_VERIFIED(canRetry = true),
    MISSING_IDENTITY(canRetry = true),
    UNDER_18(canRetry = false),

    // App-side, not backend statuses.
    NO_WALLET(canRetry = true),
    TECHNICAL(canRetry = true),
}

/** What the app does with a backend `status`. The app never decides on its own: it follows this. */
sealed interface StatusOutcome {
    data object KeepPolling : StatusOutcome
    data object Verified : StatusOutcome
    data object Confirmed : StatusOutcome
    data class Failed(val kind: ErrorKind) : StatusOutcome
}

fun outcomeOf(status: String): StatusOutcome = when (status) {
    "PENDING" -> StatusOutcome.KeepPolling
    "VERIFIED" -> StatusOutcome.Verified
    "CONFIRMED" -> StatusOutcome.Confirmed
    "DECLINED" -> StatusOutcome.Failed(ErrorKind.DECLINED)
    "EXPIRED" -> StatusOutcome.Failed(ErrorKind.EXPIRED)
    "WALLET_ERROR" -> StatusOutcome.Failed(ErrorKind.WALLET_ERROR)
    "NOT_VERIFIED" -> StatusOutcome.Failed(ErrorKind.NOT_VERIFIED)
    "UNDER_18" -> StatusOutcome.Failed(ErrorKind.UNDER_18)
    "MISSING_IDENTITY" -> StatusOutcome.Failed(ErrorKind.MISSING_IDENTITY)
    // An unknown status is never treated as success.
    else -> StatusOutcome.Failed(ErrorKind.NOT_VERIFIED)
}
