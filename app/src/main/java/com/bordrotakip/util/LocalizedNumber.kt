package com.bordrotakip.util

import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Locale
import kotlin.math.abs

private val trLocale: Locale = Locale.forLanguageTag("tr-TR")
private val trSymbols: DecimalFormatSymbols = DecimalFormatSymbols.getInstance(trLocale).apply {
    groupingSeparator = '.'
    decimalSeparator = ','
}

private fun createTwoFractionFormatter(): DecimalFormat = DecimalFormat("#,##0.00", trSymbols).apply {
    isGroupingUsed = true
    minimumFractionDigits = 2
    maximumFractionDigits = 2
}

private fun detectDecimalSeparator(
    value: String,
    allowTrailingSeparator: Boolean
): Char? {
    val commaCount = value.count { it == ',' }
    val dotCount = value.count { it == '.' }

    if (commaCount > 0 && dotCount > 0) {
        return if (value.lastIndexOf(',') > value.lastIndexOf('.')) ',' else '.'
    }

    if (commaCount == 1) {
        val index = value.lastIndexOf(',')
        val digitsAfter = value.substring(index + 1).count { it.isDigit() }
        if (digitsAfter in 1..2 || (allowTrailingSeparator && digitsAfter == 0)) return ','
    }

    if (dotCount == 1) {
        val index = value.lastIndexOf('.')
        val digitsAfter = value.substring(index + 1).count { it.isDigit() }
        if (digitsAfter in 1..2 || (allowTrailingSeparator && digitsAfter == 0)) return '.'
    }

    return null
}

private fun normalizeDecimalInput(
    raw: String,
    allowTrailingSeparator: Boolean
): String? {
    val trimmed = raw
        .replace('₺', ' ')
        .replace(Regex("\\s+"), "")
        .trim()
    if (trimmed.isBlank()) return null

    val hasNegativeSign = trimmed.contains('-')
    val unsigned = trimmed.replace("-", "")
    val decimalSeparator = detectDecimalSeparator(
        value = unsigned,
        allowTrailingSeparator = allowTrailingSeparator
    )
    val decimalSeparatorIndex = if (decimalSeparator != null) {
        unsigned.lastIndexOf(decimalSeparator)
    } else {
        -1
    }

    val integerPartSource = if (decimalSeparatorIndex >= 0) {
        unsigned.substring(0, decimalSeparatorIndex)
    } else {
        unsigned
    }
    val integerDigits = integerPartSource.filter { it.isDigit() }

    val fractionDigits = if (decimalSeparatorIndex >= 0) {
        unsigned.substring(decimalSeparatorIndex + 1).filter { it.isDigit() }.take(2)
    } else {
        ""
    }

    if (integerDigits.isBlank() && fractionDigits.isBlank()) {
        if (allowTrailingSeparator && decimalSeparatorIndex >= 0) return "0,"
        return null
    }

    val normalizedInteger = integerDigits.trimStart('0').ifBlank { "0" }
    val normalized = when {
        fractionDigits.isNotBlank() -> "$normalizedInteger,$fractionDigits"
        allowTrailingSeparator && decimalSeparatorIndex >= 0 -> "$normalizedInteger,"
        else -> normalizedInteger
    }

    return if (hasNegativeSign && normalized != "0") "-$normalized" else normalized
}

fun sanitizeDecimalInput(raw: String): String =
    normalizeDecimalInput(raw = raw, allowTrailingSeparator = true) ?: ""

fun String.toLocalizedDoubleOrNull(): Double? {
    val normalized = normalizeDecimalInput(raw = this, allowTrailingSeparator = false) ?: return null
    return normalized.replace(',', '.').toDoubleOrNull()
}

fun Double.toLocalizedInputString(): String {
    if (!isFinite()) return ""
    return formatLocalizedAmount(this)
}

fun formatLocalizedAmount(amount: Double): String {
    if (!amount.isFinite()) return "0,00"
    return createTwoFractionFormatter().format(amount)
}

fun formatLocalizedCurrency(amount: Double): String {
    val absoluteFormatted = formatLocalizedAmount(abs(amount))
    return if (amount < 0) "-₺$absoluteFormatted" else "₺$absoluteFormatted"
}
