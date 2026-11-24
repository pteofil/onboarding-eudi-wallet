package ro.banking40.team01.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import ro.banking40.team01.R

@Composable
fun LabeledValue(label: String, value: String, modifier: Modifier = Modifier) {
    Column(modifier) {
        Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.secondary)
        Text(value, style = MaterialTheme.typography.bodyLarge)
    }
}

@Composable
fun VerifiedBadge() = Pill(
    text = stringResource(R.string.verified_badge),
    container = MaterialTheme.colorScheme.primaryContainer,
    content = MaterialTheme.colorScheme.onPrimaryContainer,
)

/** Marks data from the backend's stub verifier, so it is never mistaken for a real share. */
@Composable
fun TestDataChip() = Pill(
    text = stringResource(R.string.test_data_chip),
    container = Color(0xFFFFE08A),
    content = Color(0xFF3D2E00),
)

@Composable
private fun Pill(text: String, container: Color, content: Color) {
    Surface(color = container, contentColor = content, shape = MaterialTheme.shapes.small) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
        )
    }
}
