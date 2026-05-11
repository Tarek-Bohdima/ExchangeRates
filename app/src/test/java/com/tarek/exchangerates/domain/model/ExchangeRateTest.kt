package com.tarek.exchangerates.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class ExchangeRateTest {

    private val usd = Currency.of("USD")
    private val eur = Currency.of("EUR")

    @Test
    fun `valid edge constructs`() {
        val r = ExchangeRate(usd, eur, 0.92)
        assertEquals(usd, r.from)
        assertEquals(eur, r.to)
        assertEquals(0.92, r.rate, 0.0)
    }

    @Test
    fun `rejects non-positive rate`() {
        assertThrows(IllegalArgumentException::class.java) { ExchangeRate(usd, eur, 0.0) }
        assertThrows(IllegalArgumentException::class.java) { ExchangeRate(usd, eur, -1.0) }
    }

    @Test
    fun `rejects non-finite rate`() {
        assertThrows(IllegalArgumentException::class.java) {
            ExchangeRate(usd, eur, Double.POSITIVE_INFINITY)
        }
        assertThrows(IllegalArgumentException::class.java) {
            ExchangeRate(usd, eur, Double.NaN)
        }
    }

    @Test
    fun `rejects self-loop`() {
        assertThrows(IllegalArgumentException::class.java) {
            ExchangeRate(usd, usd, 1.0)
        }
    }

    @Test
    fun `inverse swaps endpoints and reciprocates rate`() {
        val forward = ExchangeRate(usd, eur, 0.92)
        val back = forward.inverse()
        assertEquals(eur, back.from)
        assertEquals(usd, back.to)
        // 1 / 0.92 ≈ 1.0869565
        assertEquals(1.0 / 0.92, back.rate, 1e-12)
    }

    @Test
    fun `inverse is involutive`() {
        val r = ExchangeRate(usd, eur, 0.92)
        val back = r.inverse().inverse()
        assertEquals(r.from, back.from)
        assertEquals(r.to, back.to)
        assertEquals(r.rate, back.rate, 1e-12)
    }
}
