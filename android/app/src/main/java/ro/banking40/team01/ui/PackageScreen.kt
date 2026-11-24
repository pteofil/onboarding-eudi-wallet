package ro.banking40.team01.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectable
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import ro.banking40.team01.R
import ro.banking40.team01.api.Decision
import ro.banking40.team01.api.InsuranceLine
import ro.banking40.team01.api.NotEligible
import ro.banking40.team01.api.Reason
import ro.banking40.team01.ui.theme.VerifiedStartTheme

private val LADDER = listOf("Simple", "Starter", "Plus", "Premium")

/** 6 Your package: everything shown here comes from the backend's decision. */
@Composable
fun PackageScreen(
    decision: Decision,
    selected: String?,
    onSelect: (String) -> Unit,
    onContinue: () -> Unit,
) {
    val notEligible = decision.notEligible.associateBy { it.packageName }
    // Recommended first, then the other eligible packages, then the ones not available; ladder order within each.
    val names = (LADDER + decision.eligible + notEligible.keys).distinct()
        .filter { it in decision.eligible || it in notEligible }
        .sortedBy { if (it == decision.recommended) 0 else if (it in decision.eligible) 1 else 2 }

    StepLayout(title = stringResource(R.string.package_title), body = stringResource(R.string.package_body)) {
        names.forEach { name ->
            val eligible = name in decision.eligible
            PackageCard(
                name = name,
                recommended = name == decision.recommended,
                eligible = eligible,
                selected = name == selected,
                decision = decision,
                notEligibleReason = notEligible[name]?.let { reasonText(it.reason) },
                onSelect = { if (eligible) onSelect(name) },
            )
        }
        if (decision.assumptions.isNotEmpty()) {
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(stringResource(R.string.package_assumptions), style = MaterialTheme.typography.labelSmall)
                decision.assumptions.forEach {
                    Text("• ${reasonText(it, ASSUMPTION_PREFIX)}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.secondary)
                }
            }
        }
        Button(onClick = onContinue, enabled = selected != null, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.package_continue, selected.orEmpty()))
        }
    }
}

@Composable
private fun PackageCard(
    name: String,
    recommended: Boolean,
    eligible: Boolean,
    selected: Boolean,
    decision: Decision,
    notEligibleReason: String?,
    onSelect: () -> Unit,
) {
    val border = if (recommended) BorderStroke(2.dp, MaterialTheme.colorScheme.primary) else CardDefaults.outlinedCardBorder()
    OutlinedCard(
        border = border,
        modifier = Modifier
            .fillMaxWidth()
            .alpha(if (eligible) 1f else 0.5f)
            .selectable(selected = selected, enabled = eligible, role = Role.RadioButton, onClick = onSelect),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            if (recommended) {
                Text(
                    stringResource(R.string.package_recommended),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold,
                )
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(name, style = MaterialTheme.typography.titleLarge, modifier = Modifier.weight(1f))
                RadioButton(selected = selected, onClick = null, enabled = eligible)
            }
            packageDescription(name)?.let { Text(it, style = MaterialTheme.typography.bodyMedium) }
            if (!eligible) {
                Text(
                    listOfNotNull(stringResource(R.string.package_not_eligible), notEligibleReason).joinToString(": "),
                    style = MaterialTheme.typography.bodySmall,
                )
            }
            if (recommended && decision.reasons.isNotEmpty()) {
                Text(stringResource(R.string.package_why), style = MaterialTheme.typography.labelMedium)
                decision.reasons.forEach { Text("✓ ${reasonText(it)}", style = MaterialTheme.typography.bodySmall) }
            }
            if (recommended && decision.insurance.isNotEmpty()) {
                Text(stringResource(R.string.package_insurance), style = MaterialTheme.typography.labelMedium)
                decision.insurance.forEach { (key, line) -> InsuranceRow(key, line) }
            }
        }
    }
}

@Composable
private fun InsuranceRow(key: String, line: InsuranceLine) {
    val label = when (key) {
        "travel" -> stringResource(R.string.package_insurance_travel)
        "life" -> stringResource(R.string.package_insurance_life)
        else -> key.replaceFirstChar(Char::uppercase)
    }
    val status = stringResource(
        if (line.available) R.string.package_insurance_included else R.string.package_insurance_not_available,
    )
    Column {
        Text(
            "${if (line.available) "✓" else "–"} $label · $status",
            style = MaterialTheme.typography.bodyMedium,
            color = if (line.available) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.secondary,
        )
        reasonText(line.reason).takeIf { it.isNotBlank() }?.let {
            Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.secondary)
        }
    }
}

@Composable
private fun packageDescription(name: String): String? = when (name) {
    "Simple" -> stringResource(R.string.package_desc_simple)
    "Starter" -> stringResource(R.string.package_desc_starter)
    "Plus" -> stringResource(R.string.package_desc_plus)
    "Premium" -> stringResource(R.string.package_desc_premium)
    else -> null
}

@Preview(showBackground = true, heightDp = 1400)
@Composable
private fun PackagePreview() = VerifiedStartTheme {
    PackageScreen(
        decision = Decision(
            status = "ELIGIBLE",
            recommended = "Premium",
            eligible = listOf("Simple", "Plus", "Premium"),
            notEligible = listOf(NotEligible("Starter", text = "Starter is for ages 18–25.")),
            insurance = mapOf(
                "travel" to InsuranceLine(true, text = "Travel insurance is included (available up to age 70)."),
                "life" to InsuranceLine(false, text = "Life insurance isn't available at your age (up to age 80)."),
            ),
            reasons = listOf(Reason("AGE_ADULT", "You're 30, so you can open an account on your own.", age = 30)),
            assumptions = listOf(Reason("FICTIONAL", "Prices and benefits belong to a fictional bank.")),
        ),
        selected = "Plus",
        onSelect = {},
        onContinue = {},
    )
}
