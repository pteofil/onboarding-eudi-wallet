package ro.banking40.team01.ui

import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

/** "1996-03-14" -> "14 March 1996" in the device's locale; anything unparseable is shown as is. */
fun formatDate(iso: String?): String? =
    iso?.let { runCatching { LocalDate.parse(it).format(DateTimeFormatter.ofLocalizedDate(FormatStyle.LONG)) }.getOrDefault(it) }

/** Address attribute names depend on the credential: prefer `formatted`, else every value in order. */
fun addressLines(address: JsonObject): List<String> {
    (address["formatted"] as? JsonPrimitive)?.content?.takeIf { it.isNotBlank() }?.let { return listOf(it) }
    return address.values.flatMap(::values)
}

/** Any JSON value as display text: lists are joined with commas. */
fun jsonText(element: JsonElement?): String? =
    element?.let(::values)?.takeIf { it.isNotEmpty() }?.joinToString(", ")

private fun values(element: JsonElement): List<String> = when (element) {
    JsonNull -> emptyList()
    is JsonPrimitive -> listOf(element.content).filter { it.isNotBlank() }
    is JsonObject -> addressLines(element)
    is JsonArray -> element.flatMap(::values)
}
