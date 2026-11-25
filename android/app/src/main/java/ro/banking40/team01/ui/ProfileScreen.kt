package ro.banking40.team01.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import ro.banking40.team01.R
import ro.banking40.team01.api.Identity
import ro.banking40.team01.onboarding.Account
import ro.banking40.team01.onboarding.KycDeclarations
import ro.banking40.team01.onboarding.KycProfile
import ro.banking40.team01.onboarding.displayName
import ro.banking40.team01.onboarding.initials
import ro.banking40.team01.ui.theme.BrandNavy
import ro.banking40.team01.ui.theme.BrandTeal
import ro.banking40.team01.ui.theme.VerifiedStartTheme
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

/** Opened from the dashboard's profile button: who the customer is and the KYC record the onboarding produced. */
@Composable
fun ProfileScreen(account: Account, onBack: () -> Unit, onLogout: () -> Unit) {
    StepLayout(title = stringResource(R.string.profile_title)) {
        Card(Modifier.fillMaxWidth()) {
            Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier
                        .size(56.dp)
                        .clip(CircleShape)
                        .background(Brush.linearGradient(listOf(BrandTeal, BrandNavy))),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        initials(account.fullName),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                    )
                }
                Column(Modifier.padding(start = 16.dp)) {
                    account.fullName?.let {
                        Text(displayName(it), style = MaterialTheme.typography.titleLarge)
                    }
                    Text(
                        stringResource(R.string.profile_package, packageLabel(account.packageName)),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.secondary,
                    )
                }
            }
        }
        account.kyc?.let { KycRecord(it) }
        Button(onClick = onBack, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.profile_back))
        }
        OutlinedButton(onClick = onLogout, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.dashboard_logout))
        }
    }
}

/** What the wallet verified and what the customer declared, in two sections. */
@Composable
private fun KycRecord(kyc: KycProfile) {
    val verified = walletKycItems(kyc.identity, kyc.maskedCnp, kyc.address, kyc.incomeVerified)
    val declared = declaredKycItems(kyc.incomeVerified)
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    stringResource(R.string.dashboard_kyc_title),
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.weight(1f),
                )
                Surface(color = MaterialTheme.colorScheme.primary, contentColor = Color.White, shape = CircleShape) {
                    Text(
                        stringResource(R.string.dashboard_kyc_complete),
                        style = MaterialTheme.typography.labelMedium,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                    )
                }
            }
            KycSummary(stringResource(R.string.dashboard_kyc_summary, verified.size, declared.size))
            SectionLabel(stringResource(R.string.profile_wallet_section), MaterialTheme.colorScheme.primary)
            verified.forEach { KycLine(it.label, it.value, fromWallet = true) }
            HorizontalDivider()
            SectionLabel(stringResource(R.string.profile_declared_section), MaterialTheme.colorScheme.secondary)
            declared.forEach { (label, value) -> KycLine(label, value, fromWallet = false) }
            val completedOn = Instant.ofEpochMilli(kyc.completedAtMillis).atZone(ZoneId.systemDefault()).toLocalDate()
                .format(DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM))
            Text(
                stringResource(R.string.dashboard_kyc_completed, completedOn),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.secondary,
            )
        }
    }
}

@Composable
private fun SectionLabel(text: String, color: Color) =
    Text(text, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold, color = color)

@Preview(showBackground = true, heightDp = 1400)
@Composable
private fun ProfilePreview() = VerifiedStartTheme {
    ProfileScreen(
        Account(
            "Ana", "ANA IONESCU", "Plus", "RO71DEMO1210141723597382", "VS-FAE69F55", 102,
            KycProfile(
                Identity("ANA", "IONESCU", "1991-04-12", JsonPrimitive("RO")),
                "2********0014",
                JsonObject(mapOf("formatted" to JsonPrimitive("Strada Exemplului 10, 010101 București"))),
                incomeVerified = true,
                declarations = KycDeclarations(true, true, true, true, true),
                completedAtMillis = 0,
            ),
        ),
        onBack = {},
        onLogout = {},
    )
}
