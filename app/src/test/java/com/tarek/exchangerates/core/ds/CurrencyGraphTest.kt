package com.tarek.exchangerates.core.ds

import com.tarek.exchangerates.domain.model.Currency
import com.tarek.exchangerates.domain.model.ExchangeRate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CurrencyGraphTest {

    private val usd = Currency.of("USD")
    private val eur = Currency.of("EUR")
    private val gbp = Currency.of("GBP")

    @Test
    fun `empty builder produces an empty graph`() {
        val g = CurrencyGraph.builder().build()
        assertEquals(0, g.vertexCount)
        assertEquals(0, g.edgeCount)
    }

    @Test
    fun `sink vertex with no outgoing edges still appears in vertices`() {
        val g = CurrencyGraph.builder()
            .addRate(ExchangeRate(usd, eur, 0.9))
            .build()
        // EUR has no outgoing edge but must still be in vertices so BFS/Dijkstra
        // recognise it as a known node rather than throwing UnknownCurrency.
        assertTrue(g.contains(eur))
        assertTrue(g.edgesFrom(eur).isEmpty())
    }

    @Test
    fun `directRate returns the rate for a known edge`() {
        val g = CurrencyGraph.builder()
            .addRate(ExchangeRate(usd, eur, 0.9))
            .build()
        assertEquals(0.9, g.directRate(usd, eur)!!, 1e-12)
    }

    @Test
    fun `directRate returns null when no edge exists`() {
        val g = CurrencyGraph.builder()
            .addRate(ExchangeRate(usd, eur, 0.9))
            .build()
        assertNull(g.directRate(eur, gbp))
        assertNull(g.directRate(eur, usd)) // no reciprocal by default
    }

    @Test
    fun `withReciprocals adds the inverse edge for every input`() {
        val g = CurrencyGraph.builder()
            .addRate(ExchangeRate(usd, eur, 0.9))
            .withReciprocals(true)
            .build()
        assertEquals(0.9, g.directRate(usd, eur)!!, 1e-12)
        // 1 / 0.9 ≈ 1.1111...
        assertEquals(1.0 / 0.9, g.directRate(eur, usd)!!, 1e-12)
    }

    @Test
    fun `vertex count grows with new currencies regardless of direction`() {
        val g = CurrencyGraph.builder()
            .addRate(ExchangeRate(usd, eur, 0.9))
            .addRate(ExchangeRate(eur, gbp, 0.85))
            .build()
        assertEquals(setOf(usd, eur, gbp), g.vertices)
        assertEquals(3, g.vertexCount)
        assertEquals(2, g.edgeCount)
    }

    @Test
    fun `addRates accepts an iterable`() {
        val g = CurrencyGraph.builder()
            .addRates(
                listOf(
                    ExchangeRate(usd, eur, 0.9),
                    ExchangeRate(eur, gbp, 0.85),
                ),
            )
            .build()
        assertEquals(2, g.edgeCount)
    }

    @Test
    fun `contains is false for an unknown currency`() {
        val g = CurrencyGraph.builder()
            .addRate(ExchangeRate(usd, eur, 0.9))
            .build()
        assertFalse(g.contains(gbp))
    }
}
