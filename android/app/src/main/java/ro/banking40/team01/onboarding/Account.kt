package ro.banking40.team01.onboarding

/** What the dashboard shows after onboarding. Lives in memory only: a demo, not a real session. */
data class Account(
    val firstName: String?,
    val fullName: String?,
    val packageName: String,
    val iban: String,
    val reference: String,
    /** From "Onboard with EUDI Wallet" to the account being opened. */
    val onboardingSeconds: Long?,
)

/** Wallet names come in capitals ("ANA-MARIA"); show them as "Ana-Maria". */
fun displayName(raw: String): String = raw.trim().lowercase().split(" ").joinToString(" ") { word ->
    word.split("-").joinToString("-") { it.replaceFirstChar(Char::uppercase) }
}
