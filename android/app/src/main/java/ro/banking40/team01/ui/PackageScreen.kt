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

    val body = decision.recommended?.let { stringResource(R.string.pkg_recommendation, packageLabel(it)) }
    StepLayout(title = stringResource(R.string.package_title), body = body) {
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
            Text(stringResource(R.string.package_continue, selected?.let { packageLabel(it) }.orEmpty()))
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
                Column(Modifier.weight(1f)) {
                    Text(packageLabel(name), style = MaterialTheme.typography.titleLarge)
                    packageFee(name)?.let {
                        Text(it, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
                    }
                }
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
                decision.insurance.values.forEach { InsuranceRow(it) }
            }
        }
    }
}

/** The backend's code picks our copy, e.g. "Travel insurance isn't available at your age (up to age 70)." */
@Composable
private fun InsuranceRow(line: InsuranceLine) {
    Text(
        "${if (line.available) "✓" else "–"} ${reasonText(line.reason)}",
        style = MaterialTheme.typography.bodyMedium,
        color = if (line.available) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.secondary,
    )
}

/** Display name for a backend package id; unknown ids are shown as they come. */
@Composable
fun packageLabel(name: String): String = when (name) {
    "Simple" -> stringResource(R.string.pkg_simple)
    "Starter" -> stringResource(R.string.pkg_starter)
    "Plus" -> stringResource(R.string.pkg_plus)
    "Premium" -> stringResource(R.string.pkg_premium)
    else -> name
}

@Composable
fun packageDescription(name: String): String? = when (name) {
    "Simple" -> stringResource(R.string.pkg_simple_desc)
    "Starter" -> stringResource(R.string.pkg_starter_desc)
    "Plus" -> stringResource(R.string.pkg_plus_desc)
    "Premium" -> stringResource(R.string.pkg_premium_desc)
    else -> null
}

/** Monthly fee for a backend package id; unknown ids show no fee. */
@Composable
fun packageFee(name: String): String? = when (name) {
    "Simple" -> stringResource(R.string.pkg_simple_fee)
    "Starter" -> stringResource(R.string.pkg_starter_fee)
    "Plus" -> stringResource(R.string.pkg_plus_fee)
    "Premium" -> stringResource(R.string.pkg_premium_fee)
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
