package com.bordrotakip.ocr

import org.junit.Assert.assertEquals
import org.junit.Test

class PayrollOcrParserTest {

    @Test
    fun `parses July 2025 sample template`() {
        val text = """
            Bordro Tar. : TEMMUZ / 2025
            Aylık Ücret : 28.830,00
            FAZLA MESAİ
            Fazla Mesai (1) : 0 0,00
            Bayram/R.T. : 0 0,00
            TOPLAM 0,00
            SGK VE VERGİLER
            Dev. Vergi Matrahı : 161.401,29
            Vergi Matrahı : 24.505,50
            Sigorta Matrahı : 28.830,00
            Gelir Vergisi : 1.585,40
            Sigorta Kesintisi : 4.036,20
            İss.Sig. Kesintisi : 288,30
            Damga Vergisi : 21,44
            EK KESİNTİ
            BES KESİNTİSİ : 864,00
            ÖDEMELER TOPLAMI : 28.830,00
            KESİNTİLER TOPLAMI : 6.795,34
            NET ÜCRET : 22.034,66
        """.trimIndent()

        val parser = PayrollOcrParser()
        val result = parser.parse(text)

        assertEquals(2025, result.year)
        assertEquals(7, result.month)
        assertEquals(28_830.0, result.grossSalary!!, 0.001)
        assertEquals(28_830.0, result.totalEarnings!!, 0.001)
        assertEquals(0.0, result.overtimeTotal!!, 0.001)
        assertEquals(161_401.29, result.carriedTaxBase!!, 0.001)
        assertEquals(24_505.50, result.taxBase!!, 0.001)
        assertEquals(22_034.66, result.netSalary!!, 0.001)
        assertEquals(4_036.20, result.sgkEmployee!!, 0.001)
        assertEquals(288.30, result.unemployment!!, 0.001)
        assertEquals(1_585.40, result.incomeTax!!, 0.001)
        assertEquals(21.44, result.stampTax!!, 0.001)
        assertEquals(864.0, result.bes!!, 0.001)
    }

    @Test
    fun `parses overtime total when hours and amount both exist`() {
        val text = """
            Bordro Tar. : EYLÜL / 2025
            Aylık Ücret : 28.830,00
            FAZLA MESAİ
            Fazla Mesai (1) : 40 7.688,00
            Bayram/R.T. : 0 0,00
            TOPLAM 7.688,00
            SGK VE VERGİLER
            Dev. Vergi Matrahı : 213.026,21
            Vergi Matrahı : 31.040,30
            ÖDEMELER TOPLAMI : 36.518,00
            NET ÜCRET : 28.078,38
        """.trimIndent()

        val parser = PayrollOcrParser()
        val result = parser.parse(text)

        assertEquals(2025, result.year)
        assertEquals(9, result.month)
        assertEquals(28_830.0, result.grossSalary!!, 0.001)
        assertEquals(36_518.0, result.totalEarnings!!, 0.001)
        assertEquals(7_688.0, result.overtimeTotal!!, 0.001)
    }

    @Test
    fun `prefers derived gross when OCR mis-associates gross with total`() {
        val text = """
            Bordro Tar. : EYLÜL / 2025
            Aylık Ücret : 36.518,00
            Sigorta Matrahı : 36.518,00
            FAZLA MESAİ
            Fazla Mesai (1) : 40 7.688,00
            TOPLAM 7.688,00
        """.trimIndent()

        val parser = PayrollOcrParser()
        val result = parser.parse(text)

        assertEquals(28_830.0, result.grossSalary!!, 0.001)
        assertEquals(36_518.0, result.totalEarnings!!, 0.001)
        assertEquals(7_688.0, result.overtimeTotal!!, 0.001)
    }

    @Test
    fun `parses tax bases when dev and normal tax base are on same line`() {
        val text = """
            Bordro Tar. : SUBAT / 2026
            SGK VE VERGILER
            Dev. Vergi Matrahı : 29.115,33 Vergi Matrahı : 34.522,47
            ODEMELER TOPLAMI : 40.614,67
            NET UCRET : 32.279,86
        """.trimIndent()

        val parser = PayrollOcrParser()
        val result = parser.parse(text)

        assertEquals(29_115.33, result.carriedTaxBase!!, 0.001)
        assertEquals(34_522.47, result.taxBase!!, 0.001)
    }

    @Test
    fun `derives tax base from earnings and deductions when tax base label is unreadable`() {
        val text = """
            Bordro Tar. : SUBAT / 2026
            SGK VE VERGILER
            Sigorta Matrahı : 40.614,67
            Sigorta Kesintisi : 5.686,05
            Iss.Sig. Kesintisi : 406,15
            ODEMELER TOPLAMI : 40.614,67
        """.trimIndent()

        val parser = PayrollOcrParser()
        val result = parser.parse(text)

        assertEquals(34_522.47, result.taxBase!!, 0.001)
    }
}
