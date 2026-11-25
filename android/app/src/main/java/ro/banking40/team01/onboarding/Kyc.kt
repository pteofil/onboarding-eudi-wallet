package ro.banking40.team01.onboarding

import kotlinx.serialization.json.JsonObject
import ro.banking40.team01.api.Identity

/**
 * The KYC declarations no wallet can answer. Pre-ticked for the demo; kept in memory only, nothing is sent.
 * A real bank would also screen the name against sanctions and PEP lists.
 */
data class KycDeclarations(
    val notPep: Boolean = true,
    val taxResidentRomaniaOnly: Boolean = true,
    val ownBehalf: Boolean = true,
    val everydayUse: Boolean = true,
    /** Asked only when income wasn't shared; otherwise the verified income attestation answers it. */
    val salaryIncome: Boolean = true,
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
