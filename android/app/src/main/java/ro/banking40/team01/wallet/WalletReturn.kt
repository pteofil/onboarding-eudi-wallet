package ro.banking40.team01.wallet

import java.net.URLDecoder

/**
 * Parses the URI the EUDI Wallet opens when it hands the customer back to the app:
 * `ro.banking40.team01://same-device#response_code=...`.
 *
 * Pure Kotlin (no android.net.Uri) so it runs in plain JVM unit tests.
 */
object WalletReturn {
    const val SCHEME = "ro.banking40.team01"
    const val HOST = "same-device"
    private const val RESPONSE_CODE = "response_code"

    fun isWalletReturn(uri: String?): Boolean =
        uri != null && uri.startsWith("$SCHEME://$HOST", ignoreCase = true) &&
            uri.getOrNull("$SCHEME://$HOST".length).let { it == null || it in "/?#" }

    /** `response_code` from the fragment (format in the Starter Pack), falling back to the query. */
    fun responseCode(uri: String): String? {
        val fragment = uri.substringAfter('#', missingDelimiterValue = "")
        val query = uri.substringBefore('#').substringAfter('?', missingDelimiterValue = "")
        return param(fragment, RESPONSE_CODE) ?: param(query, RESPONSE_CODE)
    }

    /** The URI with every `response_code` value replaced, so the one-time code never reaches the log. */
    fun redacted(uri: String): String = uri.replace(Regex("(response_code=)[^&#]*"), "$1***")

    private fun param(part: String, name: String): String? =
        part.split('&').firstNotNullOfOrNull { pair ->
            val key = decode(pair.substringBefore('='))
            val value = decode(pair.substringAfter('=', missingDelimiterValue = ""))
            value.takeIf { key == name && it.isNotEmpty() }
        }

    // Percent-decoding only: a literal '+' in a code must stay a '+', not become a space.
    private fun decode(value: String): String =
        URLDecoder.decode(value.replace("+", "%2B"), "UTF-8") // the Charset overload is API 33+
}
