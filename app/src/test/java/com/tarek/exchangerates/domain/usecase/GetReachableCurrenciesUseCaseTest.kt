package com.tarek.exchangerates.domain.usecase

import com.tarek.exchangerates.core.ds.CurrencyGraph
import com.tarek.exchangerates.domain.model.Currency
import com.tarek.exchangerates.domain.model.ExchangeRate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class GetReachableCurrenciesUseCaseTest {

    private val usd = Currency.of("USD")
    private val eur = Currency.of("EUR")
    private val gbp = Currency.of("GBP")
    private val jpy = Currency.of("JPY")
    private val chf = Currency.of("CHF")

    private val useCase = GetReachableCurrenciesUseCase()

    @Test
    fun `returns empty set for an unknown source`() {
        val graph = CurrencyGraph.builder()
            .addRate(ExchangeRate(usd, eur, 0.9))
            .build()
        assertTrue(useCase(graph, Currency.of("XYZ")).isEmpty())
    }

    @Test
    fun `excludes the source itself`() {
        val graph = CurrencyGraph.builder()
            .addRate(ExchangeRate(usd, eur, 0.9))
            .build()
        val reachable = useCase(graph, usd)
        assertEquals(setOf(eur), reachable)
        assertTrue(usd !in reachable)
    }

    @Test
    fun `walks the whole connected component`() {
        val graph = CurrencyGraph.builder()
            .addRate(ExchangeRate(usd, eur, 0.9))
            .addRate(ExchangeRate(eur, gbp, 0.85))
            .addRate(ExchangeRate(gbp, jpy, 190.0))
            .build()
        assertEquals(setOf(eur, gbp, jpy), useCase(graph, usd))
    }

    @Test
    fun `ignores disconnected components`() {
        val graph = CurrencyGraph.builder()
            .addRate(ExchangeRate(usd, eur, 0.9))
            .addRate(ExchangeRate(gbp, jpy, 190.0)) // disjoint
            .addRate(ExchangeRate(chf, usd, 1.13))  // joins CHF into USD's component
            .build()
        val reachable = useCase(graph, usd)
        assertEquals(setOf(eur, chf), reachable)
        assertTrue(gbp !in reachable)
        assertTrue(jpy !in reachable)
    }

    @Test
    fun `directed and reverse edges are treated as connectivity-equivalent`() {
        // Union-Find models undirected connectivity. CHF -> USD is enough to put
        // CHF in USD's component, even though we never declared USD -> CHF.
        val graph = CurrencyGraph.builder()
            .addRate(ExchangeRate(chf, usd, 1.13))
            .build()
        assertEquals(setOf(chf), useCase(graph, usd))
        assertEquals(setOf(usd), useCase(graph, chf))
    }
}
