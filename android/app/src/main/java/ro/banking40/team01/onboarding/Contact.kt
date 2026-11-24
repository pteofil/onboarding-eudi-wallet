package ro.banking40.team01.onboarding

/** The only details the customer types. The OTP is simulated: no SMS is sent, any 6 digits pass. */
data class Contact(
    val phone: String = "",
    val email: String = "",
    val otpSent: Boolean = false,
    val otp: String = "",
) {
    val phoneValid: Boolean
        get() = phone.matches(Regex("""\+?[0-9 ]{9,16}""")) && phone.count(Char::isDigit) in 9..15
    val emailValid: Boolean
        get() = email.matches(Regex("""[^@\s]+@[^@\s]+\.[^@\s]+"""))
    val phoneConfirmed: Boolean
        get() = otpSent && otp.length == OTP_LENGTH && otp.all(Char::isDigit)
    val complete: Boolean
        get() = phoneValid && emailValid && phoneConfirmed

    companion object {
        const val OTP_LENGTH = 6
    }
}
