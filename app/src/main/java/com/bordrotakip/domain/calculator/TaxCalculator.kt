package com.bordrotakip.domain.calculator

import javax.inject.Inject
import java.math.BigDecimal
import java.math.RoundingMode

data class TaxBracket(
    val lowerBound: Double,
    val upperBound: Double,
    val rate: Double
) {
    val percentage: Int
        get() = (rate * 100).toInt()
}

data class TaxResult(
    val taxAmount: Double,
    val bracket: TaxBracket,
    val base: Double
)

data class TaxYearConfig(
    val year: Int,
    val brackets: List<TaxBracket>,
    val minWageGross: Double
)

/**
 * Gelir vergisi hesaplayıcı
 *
 * Notlar:
 * - Bu sınıf stateful olmamalı; aynı anda farklı yıllar için hesap yapılabilir.
 * - Tarifeler / asgari ücret gibi değerler yıllara göre değişir. Buradaki değerleri
 *   resmi kaynaklarla periyodik güncellemek gerekir.
 */
class TaxCalculator @Inject constructor() {
    companion object {
        private fun bracketsFromUpperBounds(vararg upperAndRate: Pair<Double, Double>): List<TaxBracket> {
            var lower = 0.0
            return upperAndRate.map { (upper, rate) ->
                TaxBracket(lowerBound = lower, upperBound = upper, rate = rate).also { lower = upper }
            }
        }

        // Not: 2026 değerleri projedeki referans dokümanlardan (maas_hesaplama.xlsx / 2026_BruttenNeteHesaplama.pdf)
        // alınmıştır.
        private val CONFIG_2026 = TaxYearConfig(
            year = 2026,
            brackets = bracketsFromUpperBounds(
                190_000.0 to 0.15,
                400_000.0 to 0.20,
                1_500_000.0 to 0.27,
                5_300_000.0 to 0.35,
                Double.MAX_VALUE to 0.40
            ),
            minWageGross = 33_030.04
        )

        // TODO: Aşağıdaki değerleri resmi kaynaklarla doğrula/güncelle.
        private val CONFIG_2025 = TaxYearConfig(
            year = 2025,
            brackets = bracketsFromUpperBounds(
                110_000.0 to 0.15,
                230_000.0 to 0.20,
                870_000.0 to 0.27,
                3_000_000.0 to 0.35,
                Double.MAX_VALUE to 0.40
            ),
            minWageGross = 22_104.67
        )
    }

    private val configsByYear: Map<Int, TaxYearConfig> = mapOf(
        CONFIG_2025.year to CONFIG_2025,
        CONFIG_2026.year to CONFIG_2026
    )

    private fun configForYear(year: Int): TaxYearConfig {
        return configsByYear[year] ?: CONFIG_2026
    }

    fun getMinWageGross(year: Int): Double = configForYear(year).minWageGross

    /**
     * Asgari ücret vergi matrahını hesapla (yaklaşık)
     */
    private fun getMinWageTaxBase(year: Int): Double {
        val minWageGross = getMinWageGross(year)
        val sgk = minWageGross * 0.14
        val unemp = minWageGross * 0.01
        return minWageGross - sgk - unemp
    }

    /**
     * Gelir vergisi hesapla (İstisna düşülmüş net vergi)
     */
    fun calculateIncomeTax(
        year: Int,
        cumulativeBase: Double,
        monthBase: Double,
        monthIndexInYear: Int? = null,
        isExempt: Boolean = true
    ): TaxResult {
        // 1) Çalışanın brüt vergisi
        // PDF referansı ile uyum: brüt vergi tutarı 2 haneye yuvarlanır (ROUND_HALF_UP).
        val employeeTax = calculateRawTax(year, cumulativeBase, monthBase, segmentRoundingMode = RoundingMode.HALF_UP)
        
        if (!isExempt) {
            return employeeTax
        }
        
        // 2) Asgari ücret istisnası
        // Referans (Excel/PDF) ile uyumlu: istisna, asgari ücretin yıl içindeki kümülatif matrahına göre
        // (ay sayısı üzerinden) kademeli hesaplanır. Çalışanın kendi kümülatif matrahına göre değil.
        val minWageBase = getMinWageTaxBase(year)
        val exemptBase = minOf(monthBase, minWageBase)
        val minWageCumulativeBefore = if (monthIndexInYear != null) {
            require(monthIndexInYear in 1..12) { "monthIndexInYear must be 1..12" }
            minWageBase * (monthIndexInYear - 1).toDouble()
        } else {
            cumulativeBase
        }
        // PDF referansı ile uyum: istisna vergi tutarı kademelerde 2 haneye aşağı yuvarlanır (ROUND_DOWN).
        // (Örn: 2026 Ağustos asgari ücret istisnası 5.615,10)
        val minWageExemption = calculateRawTax(
            year,
            minWageCumulativeBefore,
            exemptBase,
            segmentRoundingMode = RoundingMode.DOWN
        )
        
        // Ödenecek vergi = Çalışan Vergisi - Asgari Ücret Vergisi
        // Not: Referans tablo, bu farkı 2 haneli değerler üzerinden alır.
        val payableTax = (employeeTax.taxAmount - minWageExemption.taxAmount)
            .coerceAtLeast(0.0)
            .toMoney()
        
        return TaxResult(
            taxAmount = payableTax,
            bracket = employeeTax.bracket,
            base = monthBase
        )
    }

    /**
     * Ham vergi hesaplama (İstisnasız, kümülatif dilimli)
     */
    private fun calculateRawTax(
        year: Int,
        cumulativeBase: Double,
        monthBase: Double,
        segmentRoundingMode: RoundingMode
    ): TaxResult {
        val brackets = configForYear(year).brackets
        var remainingBase = monthBase.toBigDecimalSafe()
        var currentCumulative = cumulativeBase.toBigDecimalSafe()
        var totalTax = BigDecimal.ZERO
        var currentBracket = brackets[0]
        
        for (bracket in brackets) {
            if (remainingBase.signum() <= 0) break
            
            val limit = bracket.upperBound.toBigDecimalSafe()
            
            if (currentCumulative < limit) {
                val roomInBracket = limit - currentCumulative
                val taxableAmount = remainingBase.min(roomInBracket)
                
                val segmentTax = taxableAmount * bracket.rate.toBigDecimalSafe()
                totalTax += segmentTax.setScale(2, segmentRoundingMode)
                
                remainingBase -= taxableAmount
                currentCumulative += taxableAmount
                currentBracket = bracket
            }
        }
        
        return TaxResult(totalTax.toDouble(), currentBracket, monthBase)
    }

    /**
     * Damga vergisi istisnası hesapla
     */
    fun calculateStampTaxExemption(year: Int): Double {
        // Referans tablo ile uyum: istisna tutarı 2 haneye yuvarlanır.
        return (getMinWageGross(year) * 0.00759).toMoney()
    }

    fun getCurrentBracket(year: Int, cumulativeBase: Double): TaxBracket {
        val brackets = configForYear(year).brackets
        return brackets.firstOrNull { cumulativeBase < it.upperBound } ?: brackets.last()
    }

    fun predictNextBracketEntry(year: Int, monthlyBase: Double, currentCumulative: Double): Int? {
        var tempCumulative = currentCumulative
        val currentBracket = getCurrentBracket(year, currentCumulative)
        
        for (i in 1..12) {
            tempCumulative += monthlyBase
            val nextBracket = getCurrentBracket(year, tempCumulative)
            if (nextBracket.percentage > currentBracket.percentage) {
                return i
            }
        }
        return null
    }

    private fun Double.toBigDecimalSafe(): BigDecimal = BigDecimal.valueOf(this)

    private fun Double.toMoney(): Double =
        BigDecimal.valueOf(this).setScale(2, RoundingMode.HALF_UP).toDouble()
}
