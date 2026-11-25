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
    /** Null when the app only learned CONFIRMED (e.g. after a restart) and has no review data. */
    val kyc: KycProfile? = null,
)

/** Wallet names come in capitals ("ANA-MARIA"); show them as "Ana-Maria". */
fun displayName(raw: String): String = raw.trim().lowercase().split(" ").joinToString(" ") { word ->
    word.split("-").joinToString("-") { it.replaceFirstChar(Char::uppercase) }
}

/** "ANA-MARIA POPESCU" -> "AP": first letters of the first and last name, for the profile button. */
fun initials(fullName: String?): String {
    val words = fullName?.trim()?.split(Regex("\\s+"))?.filter { it.isNotEmpty() }.orEmpty()
    return listOfNotNull(words.firstOrNull(), words.drop(1).lastOrNull())
        .joinToString("") { it.first().uppercase() }
}
