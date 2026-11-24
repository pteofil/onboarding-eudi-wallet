package ro.banking40.team01.api

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject

@Serializable
// No defaults: the Json config doesn't encode default values, so every field is always sent.
data class CreateApplicationRequest(
    val shareIncome: Boolean,
    val returnToApp: Boolean,
    val stub: Boolean,
)

@Serializable
data class CreateApplicationResponse(
    val applicationId: String,
    val deepLink: String,
    val status: String,
)

@Serializable
data class ClaimRequest(val responseCode: String)

@Serializable
data class StatusResponse(
    val applicationId: String,
    val status: String,
    val walletStatus: String? = null,
)

@Serializable
data class ReviewResponse(
    val applicationId: String,
    val identity: Identity = Identity(),
    val personalNumericCode: String? = null,
    // Attribute names come from the credential and may change: keep them dynamic.
    val address: JsonObject = JsonObject(emptyMap()),
    val incomeShared: Boolean = false,
    val withheld: List<String> = emptyList(),
    val decision: Decision,
)

@Serializable
data class Identity(
    @SerialName("given_name") val givenName: String? = null,
    @SerialName("family_name") val familyName: String? = null,
    @SerialName("birth_date") val birthDate: String? = null,
)

@Serializable
data class Decision(
    val status: String,
    val recommended: String? = null,
    val eligible: List<String> = emptyList(),
    val notEligible: List<NotEligible> = emptyList(),
    val insurance: JsonObject = JsonObject(emptyMap()),
    val reasons: List<String> = emptyList(),
    val assumptions: List<String> = emptyList(),
)

@Serializable
data class NotEligible(
    @SerialName("package") val packageName: String,
    val reason: String? = null,
)

/** `packageName = null` is sent as `{}`: the backend then confirms its recommended package. */
@Serializable
data class ConfirmRequest(@SerialName("package") val packageName: String? = null)

@Serializable
data class ConfirmResponse(
    val applicationId: String,
    val status: String,
    @SerialName("package") val packageName: String,
    val iban: String,
    val reference: String,
)

@Serializable
data class ErrorResponse(val error: String)
