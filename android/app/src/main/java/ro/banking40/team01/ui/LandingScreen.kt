package ro.banking40.team01.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import ro.banking40.team01.R
import ro.banking40.team01.ui.theme.BrandMint
import ro.banking40.team01.ui.theme.BrandNavy
import ro.banking40.team01.ui.theme.BrandTeal
import ro.banking40.team01.ui.theme.VerifiedStartTheme

/** Animated intro before screen 1: the bolt springs in, rings pulse, the texts rise in one after another. */
@Composable
fun LandingScreen(onGetStarted: () -> Unit) {
    val intro = remember { Animatable(0f) }
    val bolt = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        launch { bolt.animateTo(1f, spring(dampingRatio = 0.45f, stiffness = 120f)) }
        intro.animateTo(1f, tween(durationMillis = 2200, easing = LinearEasing))
    }
    val infinite = rememberInfiniteTransition(label = "landing")
    val pulse by infinite.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(2400, easing = LinearEasing)),
        label = "pulse",
    )
    val drift by infinite.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(6000, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "drift",
    )

    Box(
        Modifier
            .fillMaxSize()
            .drawBehind {
                drawRect(
                    Brush.linearGradient(
                        colors = listOf(BrandTeal, BrandNavy),
                        start = Offset(size.width * drift, 0f),
                        end = Offset(size.width * (1f - drift), size.height),
                    ),
                )
            }
            .systemBarsPadding()
            .padding(24.dp),
    ) {
        Column(
            modifier = Modifier.align(Alignment.Center),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Box(Modifier.size(220.dp), contentAlignment = Alignment.Center) {
                Canvas(Modifier.fillMaxSize()) {
                    val maxRadius = size.minDimension / 2
                    repeat(3) { i ->
                        val p = (pulse + i / 3f) % 1f
                        drawCircle(
                            color = BrandMint.copy(alpha = (1f - p) * 0.4f * intro.value),
                            radius = maxRadius * (0.35f + 0.65f * p),
                            style = Stroke(width = 2.dp.toPx()),
                        )
                    }
                    drawCircle(Color.White.copy(alpha = 0.10f), radius = maxRadius * 0.42f)
                }
                FlashLogo(
                    color = BrandMint,
                    modifier = Modifier
                        .size(96.dp)
                        .graphicsLayer {
                            scaleX = bolt.value
                            scaleY = bolt.value
                            rotationZ = (1f - bolt.value) * -30f
                            alpha = bolt.value.coerceIn(0f, 1f)
                        },
                )
            }
            Spacer(Modifier.height(8.dp))
            Text(
                text = stringResource(R.string.app_name),
                style = MaterialTheme.typography.displaySmall,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                modifier = Modifier.reveal({ intro.value }, start = 0.25f),
            )
            Text(
                text = stringResource(R.string.solution_name),
                style = MaterialTheme.typography.titleMedium,
                color = BrandMint,
                modifier = Modifier.reveal({ intro.value }, start = 0.4f),
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = stringResource(R.string.landing_tagline),
                style = MaterialTheme.typography.bodyLarge,
                color = Color.White.copy(alpha = 0.85f),
                textAlign = TextAlign.Center,
                modifier = Modifier.reveal({ intro.value }, start = 0.55f),
            )
        }
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .reveal({ intro.value }, start = 0.75f),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Button(
                onClick = onGetStarted,
                colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = BrandTeal),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
            ) {
                Text(stringResource(R.string.landing_get_started), style = MaterialTheme.typography.titleMedium)
            }
            Text(
                text = stringResource(R.string.landing_footer),
                style = MaterialTheme.typography.bodySmall,
                color = Color.White.copy(alpha = 0.6f),
                textAlign = TextAlign.Center,
            )
        }
    }
}

/** Fades and lifts the element in during its slice of the intro; reads the progress only while drawing. */
private fun Modifier.reveal(progress: () -> Float, start: Float, length: Float = 0.25f) = graphicsLayer {
    val t = FastOutSlowInEasing.transform(((progress() - start) / length).coerceIn(0f, 1f))
    alpha = t
    translationY = (1f - t) * 24.dp.toPx()
}

@Preview
@Composable
private fun LandingPreview() = VerifiedStartTheme { LandingScreen {} }
