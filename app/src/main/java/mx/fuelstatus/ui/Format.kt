package mx.fuelstatus.ui

import java.math.BigDecimal
import java.math.RoundingMode
import java.text.NumberFormat
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

val MexicanLocale: Locale = Locale.forLanguageTag("es-MX")
fun money(amount: Double): String = NumberFormat.getCurrencyInstance(MexicanLocale).format(amount)
fun number(amount: Double, decimals: Int = 2): String = NumberFormat.getNumberInstance(MexicanLocale).apply {
    minimumFractionDigits = decimals; maximumFractionDigits = decimals
}.format(amount)
fun date(timestamp: Long): String = Instant.ofEpochMilli(timestamp).atZone(ZoneId.systemDefault()).format(DateTimeFormatter.ofPattern("d MMM yyyy", MexicanLocale))
fun inputNumber(value: Long, scale: Int): String = BigDecimal(value).movePointLeft(scale).stripTrailingZeros().toPlainString()
fun scaledInput(text: String, scale: Int): Long? = runCatching {
    // A single comma is a decimal separator; grouping separators are deliberately not accepted.
    text.trim().replace(',', '.').toBigDecimal().movePointRight(scale).setScale(0, RoundingMode.HALF_UP).longValueExact()
}.getOrNull()
