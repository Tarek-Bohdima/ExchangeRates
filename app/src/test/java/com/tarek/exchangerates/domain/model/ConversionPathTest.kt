package com.tarek.exchangerates.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class ConversionPathTest {

    private val usd = Currency.of("USD")
    private val eur = Currency.of("EUR")
    private val gbp = Currency.of("GBP")

    @Test
    fun `identity path has zero edges and source equals target`() {
        val path = ConversionPath(source = usd, edges = emptyList())
        assertTrue(path.isIdentity)
        assertEquals(0, path.hops)
        assertEquals(1.0, path.compositeRate, 0.0)
        assertEquals(usd, path.target)
        assertEquals(listOf(usd), path.visited)
    }

    @Test
    fun `single-edge path target is last edge to`() {
        val path = ConversionPath(
            source = usd,
            edges = listOf(ExchangeRate(usd, eur, 0.9)),
        )
        assertEquals(eur, path.target)
        assertEquals(1, path.hops)
        assertEquals(0.9, path.compositeRate, 1e-12)
        assertEquals(listOf(usd, eur), path.visited)
    }

    @Test
    fun `multi-edge path composite rate is the product`() {
        val path = ConversionPath(
            source = usd,
            edges = listOf(
                ExchangeRate(usd, eur, 0.9),
                ExchangeRate(eur, gbp, 0.85),
            ),
        )
        assertEquals(0.9 * 0.85, path.compositeRate, 1e-12)
        assertEquals(listOf(usd, eur, gbp), path.visited)
    }

    @Test
    fun `rejects path whose first edge doesn't originate at source`() {
        assertThrows(IllegalArgumentException::class.java) {
            ConversionPath(
                source = gbp,
                edges = listOf(ExchangeRate(usd, eur, 0.9)),
            )
        }
    }

    @Test
    fun `rejects disconnected path`() {
        assertThrows(IllegalArgumentException::class.java) {
            ConversionPath(
                source = usd,
                edges = listOf(
                    ExchangeRate(usd, eur, 0.9),
                    ExchangeRate(gbp, usd, 1.27), // disjoint: starts at GBP, not EUR
                ),
            )
        }
    }
}
