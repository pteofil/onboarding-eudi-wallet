package ro.banking40.team01.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ro.banking40.team01.R
import ro.banking40.team01.onboarding.Account
import ro.banking40.team01.ui.theme.BrandMint
import ro.banking40.team01.ui.theme.BrandNavy
import ro.banking40.team01.ui.theme.BrandTeal
import ro.banking40.team01.ui.theme.VerifiedStartTheme

/** After onboarding: a simple account home that marks the switch from onboarding to banking. */
@Composable
fun DashboardScreen(account: Account, onLogout: () -> Unit, onNotInDemo: () -> Unit) {
    Column(
        Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
            .navigationBarsPadding(),
    ) {
        Header(account, onLogout)
        Column(
            modifier = Modifier.padding(horizontal = 24.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            QuickActions(onNotInDemo)
            PackageSummary(account)
            Activity()
            Spacer(Modifier.height(8.dp))
        }
    }
}

@Composable
private fun Header(account: Account, onLogout: () -> Unit) {
    Box {
        // The gradient stops halfway down the card, so the card overlaps it.
        Box(
            Modifier
                .matchParentSize()
                .padding(bottom = 110.dp)
                .clip(RoundedCornerShape(bottomStart = 32.dp, bottomEnd = 32.dp))
                .background(Brush.linearGradient(listOf(BrandTeal, BrandNavy))),
        )
        Column(
            Modifier
                .statusBarsPadding()
                .padding(start = 24.dp, end = 24.dp, top = 8.dp, bottom = 20.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                FlashLogo(Modifier.size(22.dp), color = BrandMint)
                Spacer(Modifier.width(8.dp))
                Text(
                    stringResource(R.string.app_name),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    modifier = Modifier.weight(1f),
                )
                TextButton(onClick = onLogout, colors = ButtonDefaults.textButtonColors(contentColor = Color.White)) {
                    Text(stringResource(R.string.dashboard_logout))
                }
            }
            Spacer(Modifier.height(12.dp))
            Text(
                text = account.firstName?.let { stringResource(R.string.dashboard_greeting, it) }
                    ?: stringResource(R.string.dashboard_greeting_plain),
                style = MaterialTheme.typography.headlineSmall,
                color = Color.White,
            )
            Spacer(Modifier.height(8.dp))
            Text(
                stringResource(R.string.dashboard_balance_label),
                style = MaterialTheme.typography.labelLarge,
                color = Color.White.copy(alpha = 0.7f),
            )
            Text(
                stringResource(R.string.dashboard_balance),
                style = MaterialTheme.typography.displaySmall,
                fontWeight = FontWeight.Bold,
                color = Color.White,
            )
            account.onboardingSeconds?.let {
                Spacer(Modifier.height(8.dp))
                Surface(color = Color.White.copy(alpha = 0.15f), contentColor = Color.White, shape = CircleShape) {
                    Text(
                        text = "✓ " + stringResource(R.string.dashboard_opened_in, duration(it)),
                        style = MaterialTheme.typography.labelLarge,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    )
                }
            }
            Spacer(Modifier.height(20.dp))
            BankCard(account)
        }
    }
}

/** A virtual card in the package's colours: it rises in once, then a light sweep passes over it. */
@Composable
private fun BankCard(account: Account) {
    val entrance = remember { Animatable(0f) }
    LaunchedEffect(Unit) { entrance.animateTo(1f, spring(dampingRatio = 0.6f, stiffness = 60f)) }
    val shine = rememberInfiniteTransition(label = "card").animateFloat(
        initialValue = -0.5f,
        targetValue = 1.5f,
        animationSpec = infiniteRepeatable(tween(durationMillis = 3200, delayMillis = 1200, easing = LinearEasing)),
        label = "shine",
    )
    val shape = RoundedCornerShape(20.dp)
    Box(
        Modifier
            .fillMaxWidth()
            .aspectRatio(1.586f)
            .graphicsLayer {
                val t = entrance.value
                alpha = t.coerceIn(0f, 1f)
                translationY = (1f - t) * 80.dp.toPx()
                rotationX = (1f - t) * 25f
                cameraDistance = 12f * density
            }
            .shadow(16.dp, shape)
            .clip(shape)
            .background(Brush.linearGradient(cardColors(account.packageName)))
            .drawWithContent {
                drawContent()
                val x = shine.value * size.width
                drawRect(
                    Brush.linearGradient(
                        colors = listOf(Color.Transparent, Color.White.copy(alpha = 0.18f), Color.Transparent),
                        start = Offset(x - size.width * 0.3f, 0f),
                        end = Offset(x + size.width * 0.3f, size.height),
                    ),
                )
            }
            .padding(20.dp),
    ) {
        Row(Modifier.align(Alignment.TopStart).fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            FlashLogo(Modifier.size(20.dp), color = Color.White)
            Spacer(Modifier.width(6.dp))
            Text(
                stringResource(R.string.app_name),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                modifier = Modifier.weight(1f),
            )
            Text(
                packageLabel(account.packageName).uppercase(),
                style = MaterialTheme.typography.labelLarge,
                letterSpacing = 2.sp,
                color = Color.White,
            )
        }
        Box(
            Modifier
                .align(Alignment.CenterStart)
                .size(width = 44.dp, height = 32.dp)
                .clip(RoundedCornerShape(6.dp))
                .background(Color.White.copy(alpha = 0.35f)),
        )
        Column(Modifier.align(Alignment.BottomStart).fillMaxWidth()) {
            Text(
                "••••  ••••  ••••  ${account.iban.takeLast(4)}",
                style = MaterialTheme.typography.titleMedium,
                fontFamily = FontFamily.Monospace,
                color = Color.White,
            )
            Row {
                Text(
                    account.fullName?.uppercase().orEmpty(),
                    style = MaterialTheme.typography.labelMedium,
                    color = Color.White.copy(alpha = 0.85f),
                    modifier = Modifier.weight(1f),
                )
                Text(
                    stringResource(R.string.dashboard_card_virtual),
                    style = MaterialTheme.typography.labelMedium,
                    color = Color.White.copy(alpha = 0.85f),
                )
            }
        }
    }
}

@Composable
private fun QuickActions(onClick: () -> Unit) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        QuickAction("↗", stringResource(R.string.dashboard_action_send), onClick)
        QuickAction("+", stringResource(R.string.dashboard_action_add_money), onClick)
        QuickAction("▭", stringResource(R.string.dashboard_action_card), onClick)
        QuickAction("◔", stringResource(R.string.dashboard_action_savings), onClick)
    }
}

@Composable
private fun QuickAction(symbol: String, label: String, onClick: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp),
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(4.dp),
    ) {
        Box(
            Modifier
                .size(56.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primaryContainer),
            contentAlignment = Alignment.Center,
        ) {
            Text(symbol, style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.primary)
        }
        Text(label, style = MaterialTheme.typography.labelMedium)
    }
}

@Composable
private fun PackageSummary(account: Account) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(
                stringResource(R.string.dashboard_your_package, packageLabel(account.packageName)),
                style = MaterialTheme.typography.titleMedium,
            )
            packageFee(account.packageName)?.let {
                Text(it, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
            }
            packageDescription(account.packageName)?.let { Text(it, style = MaterialTheme.typography.bodyMedium) }
            LabeledValue(stringResource(R.string.welcome_iban), account.iban.chunked(4).joinToString(" "))
            Text(
                stringResource(R.string.dashboard_reference, account.reference),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.secondary,
            )
        }
    }
}

@Composable
private fun Activity() {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(stringResource(R.string.dashboard_activity), style = MaterialTheme.typography.titleMedium)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary),
                    contentAlignment = Alignment.Center,
                ) {
                    Text("✓", color = Color.White, style = MaterialTheme.typography.titleMedium)
                }
                Column(
                    Modifier
                        .weight(1f)
                        .padding(start = 12.dp),
                ) {
                    Text(stringResource(R.string.dashboard_activity_opened), style = MaterialTheme.typography.bodyLarge)
                    Text(
                        stringResource(R.string.dashboard_activity_opened_detail),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.secondary,
                    )
                }
            }
            Text(
                stringResource(R.string.dashboard_activity_empty),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.secondary,
            )
        }
    }
}

@Composable
private fun duration(seconds: Long): String =
    if (seconds < 60) {
        stringResource(R.string.duration_seconds, seconds.toInt())
    } else {
        stringResource(R.string.duration_minutes_seconds, (seconds / 60).toInt(), (seconds % 60).toInt())
    }

private fun cardColors(packageName: String): List<Color> = when (packageName) {
    "Simple" -> listOf(Color(0xFF455A64), Color(0xFF78909C))
    "Starter" -> listOf(Color(0xFF00897B), Color(0xFF00ACC1))
    "Plus" -> listOf(Color(0xFF3949AB), Color(0xFF7B1FA2))
    "Premium" -> listOf(Color(0xFF141E30), Color(0xFF3A3F5C))
    else -> listOf(BrandTeal, BrandNavy)
}

@Preview(heightDp = 1200)
@Composable
private fun DashboardPreview() = VerifiedStartTheme {
    DashboardScreen(
        Account("Ana", "ANA IONESCU", "Plus", "RO71DEMO1210141723597382", "VS-FAE69F55", 102),
        onLogout = {},
        onNotInDemo = {},
    )
}
