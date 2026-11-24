package ro.banking40.team01.onboarding

import ro.banking40.team01.api.Identity
import java.text.Normalizer

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

        /** Demo code, filled in as if the phone read it from the SMS. */
        const val DEMO_OTP = "123456"

        /** Prefilled for the demo: a fictional phone and an email built from the verified name. */
        fun demoFor(identity: Identity): Contact {
            val local = listOfNotNull(identity.givenName, identity.familyName)
                .joinToString(".") { Normalizer.normalize(it, Normalizer.Form.NFD).lowercase() }
                .replace(Regex("[^a-z0-9.]"), "")
                .ifBlank { "client" }
            return Contact(phone = "+40 712 345 678", email = "$local@example.com")
        }
    }
}
