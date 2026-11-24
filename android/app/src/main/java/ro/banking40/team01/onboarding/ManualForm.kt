package ro.banking40.team01.onboarding

/**
 * The classic path, for contrast in the demo: typed details and uploaded documents that a colleague checks by hand.
 * Prefilled with a fictional person so the demo doesn't need typing; nothing is sent anywhere.
 */
data class ManualForm(
    val firstName: String = "Andrei",
    val lastName: String = "Popa",
    val birthDate: String = "21.07.1994",
    val cnp: String = "1940721000000",
    val idDocument: String = "XY 123456",
    val street: String = "Str. Florilor 12, ap. 4",
    val city: String = "Cluj-Napoca",
    val postalCode: String = "400000",
    val county: String = "Cluj",
    val employer: String = "Example Software SRL",
    val monthlyIncome: String = "7,500",
)
