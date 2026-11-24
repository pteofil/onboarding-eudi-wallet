package ro.banking40.team01.ui

import android.annotation.SuppressLint
import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import ro.banking40.team01.api.Reason

const val REASON_PREFIX = "reason_"
const val ASSUMPTION_PREFIX = "assumption_"

/** The app's own copy for a backend reason code (`reason_income_premium`), or the backend's English text. */
@Composable
fun reasonText(reason: Reason, prefix: String = REASON_PREFIX): String {
    val context = LocalContext.current
    return remember(reason, prefix) { context.reasonText(reason, prefix) }
}

// Looked up by name so new codes and Bogdan's strings work without a code change.
@SuppressLint("DiscouragedApi")
fun Context.reasonText(reason: Reason, prefix: String): String {
    val code = reason.code ?: return reason.text
    val id = resources.getIdentifier(prefix + code.lowercase(), "string", packageName)
    if (id == 0) return reason.text
    return runCatching {
        when {
            reason.age != null -> getString(id, reason.age)
            reason.amount != null -> getString(id, reason.amount)
            else -> getString(id)
        }
    }.getOrDefault(reason.text) // a placeholder that doesn't match the value
}
