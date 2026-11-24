package ro.banking40.team01.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// Fictional bank palette (no real bank's colours).
val BrandTeal = Color(0xFF0B5D5B)
val BrandNavy = Color(0xFF0A2540)
val BrandMint = Color(0xFF5EEAD4)

private val colors = lightColorScheme(
    primary = Color(0xFF0B5D5B),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFCDEBE8),
    onPrimaryContainer = Color(0xFF00201F),
    secondary = Color(0xFF4A6361),
    background = Color(0xFFF8FAF9),
    surface = Color(0xFFF8FAF9),
    error = Color(0xFFB3261E),
)

@Composable
fun VerifiedStartTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = colors, content = content)
}
