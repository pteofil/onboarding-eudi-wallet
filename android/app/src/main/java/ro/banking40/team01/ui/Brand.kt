package ro.banking40.team01.ui

import androidx.compose.foundation.Canvas
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path

/** Flash Bank's mark: a lightning bolt, drawn so the app needs no image assets. */
@Composable
fun FlashLogo(modifier: Modifier = Modifier, color: Color = MaterialTheme.colorScheme.primary) {
    Canvas(modifier) {
        val w = size.width
        val h = size.height
        val bolt = Path().apply {
            moveTo(0.58f * w, 0.04f * h)
            lineTo(0.18f * w, 0.56f * h)
            lineTo(0.46f * w, 0.56f * h)
            lineTo(0.38f * w, 0.96f * h)
            lineTo(0.82f * w, 0.40f * h)
            lineTo(0.54f * w, 0.40f * h)
            close()
        }
        drawPath(bolt, color)
    }
}
