package ro.banking40.team01.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Checkbox
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import ro.banking40.team01.R
import ro.banking40.team01.api.Identity
import ro.banking40.team01.onboarding.KycDeclarations
import ro.banking40.team01.ui.theme.VerifiedStartTheme

/** 5b KYC: the wallet already verified identity and address; the customer confirms only what no wallet can say. */
@Composable
fun KycScreen(
    identity: Identity,
    maskedCnp: String?,
    address: JsonObject,
    incomeVerified: Boolean,
    declarations: KycDeclarations,
    onChange: (KycDeclarations) -> Unit,
    onContinue: () -> Unit,
) {
    val verified = walletKycItems(identity, maskedCnp, address, incomeVerified)
    val questions = buildList {
        add(Triple(R.string.kyc_decl_not_pep, declarations.notPep) { v: Boolean -> declarations.copy(notPep = v) })
        add(Triple(R.string.kyc_decl_tax, declarations.taxResidentRomaniaOnly) { v: Boolean -> declarations.copy(taxResidentRomaniaOnly = v) })
        add(Triple(R.string.kyc_decl_own_behalf, declarations.ownBehalf) { v: Boolean -> declarations.copy(ownBehalf = v) })
        add(Triple(R.string.kyc_decl_purpose, declarations.everydayUse) { v: Boolean -> declarations.copy(everydayUse = v) })
        if (!incomeVerified) {
            add(Triple(R.string.kyc_decl_salary, declarations.salaryIncome) { v: Boolean -> declarations.copy(salaryIncome = v) })
        }
    }
    val done = verified.size + questions.count { it.second }
    val total = verified.size + questions.size
    StepLayout(title = stringResource(R.string.kyc_title), body = stringResource(R.string.kyc_body)) {
        KycProgress(done, total)
        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        stringResource(R.string.kyc_verified_section),
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.weight(1f),
                    )
                    VerifiedBadge()
                }
                Text(
                    stringResource(R.string.kyc_verified_hint),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.secondary,
                )
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    verified.forEach { CheckChip(it.short) }
                }
            }
        }
        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(stringResource(R.string.kyc_declarations_section), style = MaterialTheme.typography.titleMedium)
                questions.forEach { (text, checked, update) ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .toggleable(value = checked, role = Role.Checkbox, onValueChange = { onChange(update(it)) }),
                    ) {
                        Checkbox(checked = checked, onCheckedChange = null)
                        Text(
                            stringResource(text),
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(start = 12.dp),
                        )
                    }
                }
            }
        }
        Text(
            stringResource(R.string.kyc_demo_note),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.secondary,
        )
        Button(
            onClick = onContinue,
            enabled = declarations.complete(incomeVerified),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(stringResource(R.string.kyc_continue))
        }
    }
}

/** One KYC detail the EUDI Wallet verified; [short] names it on the KYC screen, where the value isn't repeated. */
data class KycItem(val label: String, val value: String, val short: String = label)

@Composable
fun walletKycItems(identity: Identity, maskedCnp: String?, address: JsonObject, incomeVerified: Boolean): List<KycItem> =
    buildList {
        listOfNotNull(identity.givenName, identity.familyName).joinToString(" ").takeIf { it.isNotBlank() }
            ?.let { add(KycItem(stringResource(R.string.kyc_name), it)) }
        formatDate(identity.birthDate)?.let { add(KycItem(stringResource(R.string.kyc_birth_date), it)) }
        maskedCnp?.let { add(KycItem(stringResource(R.string.kyc_cnp), it, stringResource(R.string.kyc_cnp_short))) }
        jsonText(identity.nationality)?.let { add(KycItem(stringResource(R.string.kyc_nationality), it)) }
        addressLines(address).takeIf { it.isNotEmpty() }
            ?.let { add(KycItem(stringResource(R.string.kyc_address), it.joinToString(", "))) }
        if (incomeVerified) {
            add(KycItem(stringResource(R.string.kyc_source_of_funds), stringResource(R.string.kyc_value_salary_verified)))
        }
    }

/** KYC answers the customer declared, as (label, value). */
@Composable
fun declaredKycItems(incomeVerified: Boolean): List<Pair<String, String>> = buildList {
    if (!incomeVerified) add(stringResource(R.string.kyc_source_of_funds) to stringResource(R.string.kyc_value_salary))
    add(stringResource(R.string.kyc_pep) to stringResource(R.string.kyc_value_no))
    add(stringResource(R.string.kyc_tax_residency) to stringResource(R.string.kyc_value_romania_only))
    add(stringResource(R.string.kyc_acting_for) to stringResource(R.string.kyc_value_myself))
    add(stringResource(R.string.kyc_purpose) to stringResource(R.string.kyc_value_everyday))
}

@Composable
private fun KycProgress(done: Int, total: Int) {
    Surface(
        color = MaterialTheme.colorScheme.primaryContainer,
        contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
        shape = MaterialTheme.shapes.medium,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(horizontal = 14.dp, vertical = 12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                stringResource(R.string.kyc_progress, done, total),
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
            )
            LinearProgressIndicator(progress = { done / total.toFloat() }, modifier = Modifier.fillMaxWidth())
        }
    }
}

@Composable
private fun CheckChip(text: String) {
    Surface(
        color = MaterialTheme.colorScheme.surface,
        contentColor = MaterialTheme.colorScheme.primary,
        shape = MaterialTheme.shapes.small,
    ) {
        Text(
            "✓ $text",
            style = MaterialTheme.typography.labelLarge,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
        )
    }
}

@Composable
fun KycSummary(text: String) {
    Surface(
        color = MaterialTheme.colorScheme.primaryContainer,
        contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
        shape = MaterialTheme.shapes.medium,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Text(
            text,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
        )
    }
}

/** One KYC detail with where it came from: the wallet (verified) or the customer (declared). */
@Composable
fun KycLine(label: String, value: String, fromWallet: Boolean) {
    Row(verticalAlignment = Alignment.Top) {
        Text(
            if (fromWallet) "✓" else "•",
            style = MaterialTheme.typography.bodyMedium,
            color = if (fromWallet) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary,
            modifier = Modifier.width(24.dp),
        )
        Column(Modifier.weight(1f)) {
            Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.secondary)
            Text(value, style = MaterialTheme.typography.bodyLarge)
        }
        Text(
            stringResource(if (fromWallet) R.string.kyc_source_wallet else R.string.kyc_source_declared),
            style = MaterialTheme.typography.labelSmall,
            color = if (fromWallet) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary,
            modifier = Modifier.padding(start = 8.dp),
        )
    }
}

@Preview(showBackground = true, heightDp = 1300)
@Composable
private fun KycPreview() = VerifiedStartTheme {
    KycScreen(
        identity = Identity("ANA", "IONESCU", "1991-04-12", JsonPrimitive("RO")),
        maskedCnp = "2********0014",
        address = JsonObject(mapOf("formatted" to JsonPrimitive("Strada Exemplului 10, 010101 București"))),
        incomeVerified = true,
        declarations = KycDeclarations(),
        onChange = {},
        onContinue = {},
    )
}
