package com.bordrotakip.ocr

import java.util.Locale
import javax.inject.Inject

data class PayrollOcrResult(
    val grossSalary: Double? = null, // Aylık Ücret
    val totalEarnings: Double? = null, // Ödemeler Toplamı
    val overtimeTotal: Double? = null, // Fazla Mesai Toplam
    val netSalary: Double? = null,
    val carriedTaxBase: Double? = null, // Dev. Vergi Matrahı (devreden)
    val taxBase: Double? = null,
    val sgkEmployee: Double? = null,
    val unemployment: Double? = null,
    val incomeTax: Double? = null,
    val stampTax: Double? = null,
    val bes: Double? = null,
    val year: Int? = null,
    val month: Int? = null
)

/**
 * Bordro OCR çıktısından temel alanları çekmek için basit parser (heuristic).
 *
 * Not: Bordro şablonları firmadan firmaya değişir; bu yüzden bu parser %100 değildir.
 * Ama “onayla/düzelt” ekranı için iyi bir başlangıç sağlar.
 */
class PayrollOcrParser @Inject constructor() {
    fun parse(text: String): PayrollOcrResult {
        val lines = text
            .lineSequence()
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .toList()

        val matchLines = lines.map { normalizeForMatch(it) }

        val grossSalaryCandidate = findAmountByKey(
            matchLines = matchLines,
            key = Regex("\\bAYLIK\\s+UCRET\\b")
        )

        val totalEarnings = findAmountByKey(
            matchLines = matchLines,
            key = Regex("\\bODEMELER\\s+TOPLAMI\\b")
        ) ?: findAmountByKey(
            matchLines = matchLines,
            key = Regex("\\bSIGORTA\\s+MATRAH(I)?\\b")
        )

        val overtimeTotal = findOvertimeTotal(matchLines)

        val grossSalaryFromTotal = run {
            if (totalEarnings != null && overtimeTotal != null) {
                (totalEarnings - overtimeTotal).takeIf { it > 0.0 }
            } else null
        }

        val grossSalaryFromDaily = run {
            val daily = findAmountByKey(
                matchLines = matchLines,
                key = Regex("\\bGUNLUK\\b.*\\bUCRET\\b"),
                lookAheadLines = 2
            )
            val paidDays = findAmountByKey(
                matchLines = matchLines,
                key = Regex("\\bUCRET\\s+GUNU\\b"),
                lookAheadLines = 2
            )
            if (daily != null && paidDays != null && paidDays > 0.0) {
                daily * paidDays
            } else null
        }

        val grossSalary = chooseGrossSalary(
            candidate = grossSalaryCandidate,
            totalEarnings = totalEarnings,
            overtimeTotal = overtimeTotal,
            derivedFromTotals = grossSalaryFromTotal,
            derivedFromDaily = grossSalaryFromDaily
        )

        val netSalary = findAmountByKey(
            matchLines = matchLines,
            key = Regex("\\bNET\\s+UCRET\\b")
        ) ?: findAmountByKey(
            matchLines = matchLines,
            key = Regex("\\bODENECEK\\s+TUTAR\\b")
        )

        val carriedTaxBase = findCarriedTaxBase(matchLines)
        val taxBaseFromText = findTaxBase(matchLines)

        val sgkEmployee = findAmountByKey(
            matchLines = matchLines,
            key = Regex("\\bSIGORTA\\s+KESINTISI\\b")
        )
        val unemployment = findAmountByKey(
            matchLines = matchLines,
            key = Regex("(IS\\s*SIG\\s*KESINTISI|ISS\\s*SIG\\s*KESINTISI|ISSIZLIK\\s+KESINTISI)"),
            lookAheadLines = 2
        )
        val incomeTax = findAmountByKey(
            matchLines = matchLines,
            key = Regex("\\bGELIR\\s+VERGISI\\b")
        )
        val stampTax = findAmountByKey(
            matchLines = matchLines,
            key = Regex("\\bDAMGA\\s+VERGISI\\b")
        )
        val bes = findAmountByKey(
            matchLines = matchLines,
            key = Regex("\\bBES\\b.*\\bKESINTI")
        )

        val taxBase = chooseTaxBase(
            taxBaseFromText = taxBaseFromText,
            carriedTaxBase = carriedTaxBase,
            totalEarnings = totalEarnings,
            sgkEmployee = sgkEmployee,
            unemployment = unemployment
        )

        val (month, year) = findMonthYear(matchLines)

        return PayrollOcrResult(
            grossSalary = grossSalary,
            totalEarnings = totalEarnings,
            overtimeTotal = overtimeTotal,
            netSalary = netSalary,
            carriedTaxBase = carriedTaxBase,
            taxBase = taxBase,
            sgkEmployee = sgkEmployee,
            unemployment = unemployment,
            incomeTax = incomeTax,
            stampTax = stampTax,
            bes = bes,
            year = year,
            month = month
        )
    }

    private fun findMonthYear(lines: List<String>): Pair<Int?, Int?> {
        val months = listOf(
            "OCAK" to 1,
            "ŞUBAT" to 2,
            "SUBAT" to 2,
            "MART" to 3,
            "NİSAN" to 4,
            "NISAN" to 4,
            "MAYIS" to 5,
            "HAZİRAN" to 6,
            "HAZIRAN" to 6,
            "TEMMUZ" to 7,
            "AĞUSTOS" to 8,
            "AGUSTOS" to 8,
            "EYLÜL" to 9,
            "EYLUL" to 9,
            "EKİM" to 10,
            "EKIM" to 10,
            "KASIM" to 11,
            "ARALIK" to 12
        )

        val yearRegex = Regex("\\b(20\\d{2})\\b")

        for (line in lines) {
            val year = yearRegex.find(line)?.groupValues?.getOrNull(1)?.toIntOrNull()
            if (year == null) continue

            val month = months.firstOrNull { (name, _) -> line.contains(name) }?.second
            if (month != null) return month to year
        }

        return null to null
    }

    private val amountRegex: Regex = Regex(
        // 24.505,50  | 24505,50 | 24 505,50 | 288,30 | 21,44 | 28830
        "(?<!\\d)(\\d{1,3}(?:[\\.\\s]\\d{3})*(?:,\\d{1,2})?|\\d+(?:,\\d{1,2})?)(?!\\d)"
    )

    private fun chooseGrossSalary(
        candidate: Double?,
        totalEarnings: Double?,
        overtimeTotal: Double?,
        derivedFromTotals: Double?,
        derivedFromDaily: Double?
    ): Double? {
        if (candidate == null) return derivedFromTotals ?: derivedFromDaily

        val hasOvertime = overtimeTotal?.let { it > 0.0 } == true

        val candidateTooHighForTotal = totalEarnings?.let { candidate > (it + 1.0) } == true
        val candidateEqualsTotalWithOvertime =
            hasOvertime && totalEarnings != null && kotlin.math.abs(candidate - totalEarnings) <= 1.0

        if (candidateTooHighForTotal || candidateEqualsTotalWithOvertime) {
            return derivedFromTotals ?: derivedFromDaily ?: candidate
        }

        return candidate
    }

    private fun findAmountByKey(
        matchLines: List<String>,
        key: Regex,
        exclude: Regex? = null,
        lookAheadLines: Int = 1
    ): Double? {
        for (i in matchLines.indices) {
            val line = matchLines[i]
            val keyMatch = key.find(line) ?: continue
            if (exclude != null && exclude.containsMatchIn(line)) continue

            extractBestAmountNearKey(line, keyMatch.range)?.let { return it }

            for (j in 1..lookAheadLines) {
                val idx = i + j
                if (idx >= matchLines.size) break
                extractAllAmounts(matchLines[idx]).firstOrNull()?.let { return it }
            }
        }
        return null
    }

    private fun findOvertimeTotal(matchLines: List<String>): Double? {
        val overtimeIndices = matchLines.indices.filter { idx ->
            val line = matchLines[idx]
            line.contains("FAZLA") && line.contains("MESA")
        }

        // 1) En güvenlisi: "Fazla Mesai (1)" satırlarında saat + tutar birlikte olur, satırın max tutarı genelde "tutar"dır.
        val overtimeFromDetailLines = overtimeIndices
            .mapNotNull { idx ->
                val line = matchLines[idx]
                val matches = amountRegex.findAll(line).toList()
                if (matches.isEmpty()) return@mapNotNull null

                val moneyLike = matches
                    .filter { it.value.contains(",") }
                    .mapNotNull { parseTrAmount(it.value) }

                val chosen = if (moneyLike.isNotEmpty()) {
                    moneyLike.maxOrNull()
                } else {
                    matches.mapNotNull { parseTrAmount(it.value) }.maxOrNull()
                }

                chosen
            }
            .maxOrNull()
        if (overtimeFromDetailLines != null) return overtimeFromDetailLines

        // 2) Header bulunmuşsa yakındaki "TOPLAM" satırını ara.
        val headerIdx = matchLines.indexOfFirst { it.contains("FAZLA") && it.contains("MESA") }
        if (headerIdx != -1) {
            val windowStart = headerIdx
            val windowEndExclusive = minOf(headerIdx + 15, matchLines.size)
            val toplamCandidates = (windowStart until windowEndExclusive)
                .filter { idx -> matchLines[idx].contains("TOPLAM") }
                .mapNotNull { idx -> extractAllAmounts(matchLines[idx]).maxOrNull() }

            val best = toplamCandidates.maxOrNull()
            if (best != null) return best
        }

        return null
    }

    private fun findCarriedTaxBase(matchLines: List<String>): Double? {
        val key = Regex("\\bVERGI\\s+MATRAH[I1]?\\b")
        for (i in matchLines.indices) {
            val line = matchLines[i]
            val matches = key.findAll(line).toList()
            if (matches.isEmpty()) continue

            for (match in matches) {
                if (!isDevMatch(line, match.range)) continue
                findAmountForMatch(matchLines, i, match.range, lookAheadLines = 2)?.let { return it }
            }
        }

        return findAmountByKey(
            matchLines = matchLines,
            key = Regex("\\bDEV\\b.*\\bVERGI\\b.*\\bMATRAH")
        )
    }

    private fun findTaxBase(matchLines: List<String>): Double? {
        findAmountByKey(
            matchLines = matchLines,
            key = Regex("\\bGELIR\\s+VERGI\\w*\\s+MATRAH[I1]?\\b"),
            lookAheadLines = 2
        )?.let { return it }

        val key = Regex("\\bVERGI\\s+MATRAH[I1]?\\b")
        for (i in matchLines.indices) {
            val line = matchLines[i]
            val matches = key.findAll(line).toList()
            if (matches.isEmpty()) continue

            val nonDevMatches = matches.filterNot { isDevMatch(line, it.range) }
            if (nonDevMatches.isEmpty()) continue

            for (match in nonDevMatches.asReversed()) {
                findAmountForMatch(matchLines, i, match.range, lookAheadLines = 2)?.let { return it }
            }
        }

        return findAmountByKey(
            matchLines = matchLines,
            key = Regex("\\bGV\\b.*\\bMATRAH"),
            lookAheadLines = 2
        )
    }

    private fun isDevMatch(line: String, keyRange: IntRange): Boolean {
        val prefixStart = maxOf(0, keyRange.first - 10)
        val prefix = line.substring(prefixStart, keyRange.first)
        return prefix.contains("DEV")
    }

    private fun findAmountForMatch(
        matchLines: List<String>,
        lineIndex: Int,
        keyRange: IntRange,
        lookAheadLines: Int
    ): Double? {
        extractBestAmountNearKey(matchLines[lineIndex], keyRange)?.let { return it }

        for (j in 1..lookAheadLines) {
            val idx = lineIndex + j
            if (idx >= matchLines.size) break
            extractAllAmounts(matchLines[idx]).firstOrNull()?.let { return it }
        }
        return null
    }

    private fun chooseTaxBase(
        taxBaseFromText: Double?,
        carriedTaxBase: Double?,
        totalEarnings: Double?,
        sgkEmployee: Double?,
        unemployment: Double?
    ): Double? {
        val derived = deriveTaxBaseFromDeductions(totalEarnings, sgkEmployee, unemployment)
        if (taxBaseFromText == null) return derived

        if (totalEarnings != null) {
            val implausible =
                taxBaseFromText > (totalEarnings + 1.0) || taxBaseFromText < (totalEarnings * 0.60)
            if (implausible) return derived ?: taxBaseFromText
        }

        if (carriedTaxBase != null && kotlin.math.abs(taxBaseFromText - carriedTaxBase) <= 1.0) {
            return derived ?: taxBaseFromText
        }

        return taxBaseFromText
    }

    private fun deriveTaxBaseFromDeductions(
        totalEarnings: Double?,
        sgkEmployee: Double?,
        unemployment: Double?
    ): Double? {
        if (totalEarnings == null || sgkEmployee == null || totalEarnings <= 0.0) return null

        if (unemployment != null) {
            return roundMoney((totalEarnings - sgkEmployee - unemployment).coerceAtLeast(0.0))
        }

        val sgkRate = sgkEmployee / totalEarnings
        return when {
            sgkRate in 0.13..0.15 -> roundMoney((totalEarnings - sgkEmployee - (totalEarnings * 0.01)).coerceAtLeast(0.0))
            sgkRate in 0.07..0.08 -> roundMoney((totalEarnings - sgkEmployee).coerceAtLeast(0.0))
            else -> null
        }
    }

    private fun roundMoney(value: Double): Double {
        return kotlin.math.round(value * 100.0) / 100.0
    }

    private fun extractAllAmounts(line: String): List<Double> {
        return amountRegex.findAll(line).mapNotNull { match ->
            parseTrAmount(match.value)
        }.toList()
    }

    private fun extractBestAmountNearKey(line: String, keyRange: IntRange): Double? {
        val matches = amountRegex.findAll(line).toList()
        if (matches.isEmpty()) return null

        val keyStart = keyRange.first
        val keyEndExclusive = keyRange.last + 1

        var bestValue: Double? = null
        var bestDistance: Int? = null

        for (match in matches) {
            val value = parseTrAmount(match.value) ?: continue
            val amountStart = match.range.first
            val amountEndExclusive = match.range.last + 1

            val distance = when {
                amountStart >= keyEndExclusive -> amountStart - keyEndExclusive
                amountEndExclusive <= keyStart -> keyStart - amountEndExclusive
                else -> 0
            }

            val shouldReplace = when {
                bestDistance == null -> true
                distance < bestDistance!! -> true
                distance == bestDistance!! && bestValue != null && value < bestValue!! -> true
                else -> false
            }

            if (shouldReplace) {
                bestDistance = distance
                bestValue = value
            }
        }

        return bestValue
    }

    private fun parseTrAmount(raw: String): Double? {
        val cleaned = raw
            .replace(" ", "")
            .replace(".", "")
            .replace(",", ".")
            .trim()

        return cleaned.toDoubleOrNull()
    }

    private fun normalizeForMatch(line: String): String {
        return line
            .uppercase(Locale.forLanguageTag("tr-TR"))
            .replace("İ", "I")
            .replace("Ş", "S")
            .replace("Ğ", "G")
            .replace("Ü", "U")
            .replace("Ö", "O")
            .replace("Ç", "C")
            .replace(".", " ")
            .replace("/", " ")
            .replace(":", " ")
            .replace(Regex("\\s+"), " ")
            .trim()
    }
}
