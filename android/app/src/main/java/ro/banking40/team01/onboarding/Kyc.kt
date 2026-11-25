package ro.banking40.team01.onboarding

import kotlinx.serialization.json.JsonObject
import ro.banking40.team01.api.Identity

/**
 * The KYC declarations no wallet can answer. Unticked until the customer confirms each one; kept in memory only, nothing is sent.
 * A real bank would also screen the name against sanctions and PEP lists.
 */
data class KycDeclarations(
    val notPep: Boolean = false,
    val taxResidentRomaniaOnly: Boolean = false,
    val ownBehalf: Boolean = false,
    val everydayUse: Boolean = false,
    /** Asked only when income wasn't shared; otherwise the verified income attestation answers it. */
    val salaryIncome: Boolean = false,
) {
    fun complete(incomeVerified: Boolean): Boolean =
        notPep && taxResidentRomaniaOnly && ownBehalf && everydayUse && (incomeVerified || salaryIncome)
}

/** The customer's KYC record as the dashboard shows it: what the wallet verified plus what they declared. */
data class KycProfile(
    val identity: Identity,
    val maskedCnp: String?,
    val address: JsonObject,
    val incomeVerified: Boolean,
    val declarations: KycDeclarations,
    val completedAtMillis: Long,
)
