package com.tarek.exchangerates.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test
import java.math.BigDecimal

class MoneyTest {

    private val usd = Currency.of("USD")
    private val eur = Currency.of("EUR")

    @Test
    fun `rejects negative amount`() {
        assertThrows(IllegalArgumentException::class.java) {
            Money(BigDecimal("-1.00"), usd)
        }
    }

    @Test
    fun `accepts zero amount`() {
        Money(BigDecimal.ZERO, usd) // does not throw
    }

    @Test
    fun `convertedTo multiplies amount by rate and tags new currency`() {
        val input = Money.of("100", usd)
        val output = input.convertedTo(target = eur, rate = 0.92)
        assertEquals(eur, output.currency)
        // 100 * 0.92 = 92 (within FP tolerance after MathContext.DECIMAL64)
        assertEquals(0, output.amount.compareTo(BigDecimal("92.0")))
    }

    @Test
    fun `times multiplies in-place keeping currency`() {
        val out = Money.of("50", usd) * 2.5
        assertEquals(usd, out.currency)
        assertEquals(0, out.amount.compareTo(BigDecimal("125.0")))
    }

    @Test
    fun `rounded honours the default display scale`() {
        val out = Money(BigDecimal("100.123456789"), usd).rounded()
        assertEquals(BigDecimal("100.1235"), out.amount) // HALF_EVEN at scale 4
    }

    @Test
    fun `comparing same-currency money compares amounts`() {
        assertTrue(Money.of("10", usd) < Money.of("20", usd))
        assertTrue(Money.of("100", usd) > Money.of("50", usd))
    }

    @Test
    fun `comparing different-currency money throws`() {
        assertThrows(IllegalArgumentException::class.java) {
            Money.of("10", usd).compareTo(Money.of("10", eur))
        }
    }

    @Test
    fun `toString renders amount and currency`() {
        // 100.0000 USD (HALF_EVEN to 4 places)
        assertEquals("100.0000 USD", Money.of("100", usd).toString())
    }
}
