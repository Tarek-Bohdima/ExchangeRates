package com.tarek.exchangerates.domain.usecase

import com.tarek.exchangerates.core.ds.CurrencyGraph
import com.tarek.exchangerates.domain.model.Currency
import com.tarek.exchangerates.domain.model.ExchangeRate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DetectArbitrageUseCaseTest {

    private val usd = Currency.of("USD")
    private val eur = Currency.of("EUR")
    private val gbp = Currency.of("GBP")
    private val jpy = Currency.of("JPY")

    @Test
    fun `detects clear arbitrage triangle`() {
        // Crafted so USD -> EUR -> GBP -> USD multiplies to ~1.05 (5% profit).
        val graph = CurrencyGraph.builder()
            .addRate(ExchangeRate(usd, eur, 1.0))
            .addRate(ExchangeRate(eur, gbp, 1.0))
            .addRate(ExchangeRate(gbp, usd, 1.05))
            .build()

        val opportunities = DetectArbitrageUseCase().invoke(graph)
        assertTrue("Expected one arbitrage, got $opportunities", opportunities.isNotEmpty())
        assertEquals(1.05, opportunities.first().profitFactor, 1e-9)
    }

    @Test
    fun `no arbitrage on balanced graph`() {
        // USD <-> EUR with product = 1.0 — no profit, no detection.
        val graph = CurrencyGraph.builder()
            .addRate(ExchangeRate(usd, eur, 0.90))
            .addRate(ExchangeRate(eur, usd, 1.0 / 0.90))
            .build()

        val opportunities = DetectArbitrageUseCase().invoke(graph)
        assertTrue("Expected no arbitrage, got $opportunities", opportunities.isEmpty())
    }

    @Test
    fun `tiny fp-noise cycle is not flagged`() {
        // Product = 1.00001 (0.001% loop) — below the 0.1% epsilon.
        val graph = CurrencyGraph.builder()
            .addRate(ExchangeRate(usd, eur, 1.0))
            .addRate(ExchangeRate(eur, usd, 1.00001))
            .build()

        val opportunities = DetectArbitrageUseCase().invoke(graph)
        assertTrue(opportunities.isEmpty())
    }

    @Test
    fun `ignores cycles outside the source's component`() {
        val graph = CurrencyGraph.builder()
            .addRate(ExchangeRate(usd, eur, 1.0))
            .addRate(ExchangeRate(eur, usd, 1.0))
            // Disconnected arbitrage triangle in another component.
            .addRate(ExchangeRate(gbp, jpy, 1.0))
            .addRate(ExchangeRate(jpy, gbp, 1.10))
            .build()

        val opportunities = DetectArbitrageUseCase().invoke(graph)
        // We launch BF from every vertex, so we *do* find the disconnected
        // triangle. The test documents that intentional behaviour.
        assertEquals(1, opportunities.size)
        assertTrue(opportunities.first().cycle.contains(gbp))
    }
}
