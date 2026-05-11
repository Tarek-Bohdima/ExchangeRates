package com.tarek.exchangerates.core.algorithm

import com.tarek.exchangerates.core.ds.CurrencyGraph
import com.tarek.exchangerates.domain.model.Currency
import com.tarek.exchangerates.domain.model.ExchangeRate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Behaviour parity tests for every strategy.
 *
 * On an arbitrage-free graph, BFS may pick a different *path* than Dijkstra
 * (it minimises hops, not rate), but Dijkstra, Bellman-Ford, and
 * Floyd-Warshall must all agree on the *best composite rate*. That single
 * invariant catches the most common algorithm bugs in one assertion.
 */
class PathFindingStrategyTest {

    private val usd = Currency.of("USD")
    private val eur = Currency.of("EUR")
    private val gbp = Currency.of("GBP")
    private val jpy = Currency.of("JPY")
    private val chf = Currency.of("CHF")

    private val noArbitrageGraph = CurrencyGraph.builder()
        .addRate(ExchangeRate(usd, eur, 0.90))
        .addRate(ExchangeRate(usd, gbp, 0.78))
        .addRate(ExchangeRate(eur, gbp, 0.85))
        .addRate(ExchangeRate(gbp, jpy, 190.0))
        .addRate(ExchangeRate(eur, jpy, 165.0))
        .addRate(ExchangeRate(chf, usd, 1.13))
        .build()

    // ===== Direct =====

    @Test
    fun `direct returns single-edge path for an existing direct rate`() {
        val path = DirectLookupStrategy().findPath(noArbitrageGraph, usd, eur)
        assertNotNull(path)
        assertEquals(1, path!!.hops)
        assertEquals(0.90, path.compositeRate, 1e-12)
    }

    @Test
    fun `direct returns null when no direct edge exists`() {
        assertNull(DirectLookupStrategy().findPath(noArbitrageGraph, usd, jpy))
    }

    @Test
    fun `direct returns null for same-source-and-target identity`() {
        assertNull(DirectLookupStrategy().findPath(noArbitrageGraph, usd, usd))
    }

    @Test
    fun `direct returns null for an unknown currency`() {
        val xyz = Currency.of("XYZ")
        assertNull(DirectLookupStrategy().findPath(noArbitrageGraph, xyz, usd))
        assertNull(DirectLookupStrategy().findPath(noArbitrageGraph, usd, xyz))
    }

    // ===== BFS =====

    @Test
    fun `bfs finds the minimum-hop path`() {
        // USD -> JPY: direct edge does not exist. Two-hop options:
        // USD->GBP->JPY (0.78 * 190 = 148.2) and USD->EUR->JPY (0.90 * 165 = 148.5).
        // BFS guarantees 2 hops; specific path is not the contract.
        val path = BfsPathFindingStrategy().findPath(noArbitrageGraph, usd, jpy)
        assertNotNull(path)
        assertEquals(2, path!!.hops)
    }

    @Test
    fun `bfs returns null for unreachable target`() {
        val isolated = CurrencyGraph.builder()
            .addRate(ExchangeRate(usd, eur, 0.9))
            .addRate(ExchangeRate(jpy, gbp, 0.005))
            .build()
        // USD and GBP are in disconnected components.
        assertNull(BfsPathFindingStrategy().findPath(isolated, usd, gbp))
    }

    @Test
    fun `bfs may choose a different path than dijkstra when both are minimum-hop`() {
        val bfs = BfsPathFindingStrategy().findPath(noArbitrageGraph, usd, jpy)!!
        assertEquals(2, bfs.hops)
        // BFS picks *some* 2-hop path; we don't assert which.
        assertTrue(bfs.compositeRate == 148.5 || bfs.compositeRate == 148.2)
    }

    // ===== Dijkstra / Bellman-Ford / Floyd-Warshall agreement =====

    @Test
    fun `dijkstra and bellman-ford agree on best rate`() {
        val d = DijkstraPathFindingStrategy().findPath(noArbitrageGraph, usd, jpy)
        val b = BellmanFordPathFindingStrategy().findPath(noArbitrageGraph, usd, jpy)
        assertNotNull(d)
        assertNotNull(b)
        assertEquals(d!!.compositeRate, b!!.compositeRate, 1e-9)
    }

    @Test
    fun `floyd-warshall agrees with dijkstra on best rate`() {
        val d = DijkstraPathFindingStrategy().findPath(noArbitrageGraph, usd, jpy)
        val fw = FloydWarshallPathFindingStrategy().findPath(noArbitrageGraph, usd, jpy)
        assertNotNull(d)
        assertNotNull(fw)
        assertEquals(d!!.compositeRate, fw!!.compositeRate, 1e-9)
    }

    @Test
    fun `dijkstra picks the higher-product two-hop path over the lower-product one`() {
        // USD->EUR->JPY = 0.90*165 = 148.5  (better)
        // USD->GBP->JPY = 0.78*190 = 148.2
        val path = DijkstraPathFindingStrategy().findPath(noArbitrageGraph, usd, jpy)!!
        assertEquals(148.5, path.compositeRate, 1e-9)
    }

    // ===== Bellman-Ford under arbitrage =====

    @Test
    fun `bellman-ford returns null when graph contains arbitrage`() {
        // Triangle product = 1.0 * 1.0 * 1.10 = 1.10 → +10% arbitrage.
        val arbitrageGraph = CurrencyGraph.builder()
            .addRate(ExchangeRate(usd, eur, 1.0))
            .addRate(ExchangeRate(eur, gbp, 1.0))
            .addRate(ExchangeRate(gbp, usd, 1.10))
            .build()
        // V-th relaxation pass detects the negative cycle in -log space and
        // the strategy bails out rather than returning an unstable answer.
        assertNull(BellmanFordPathFindingStrategy().findPath(arbitrageGraph, usd, gbp))
    }

    // ===== Floyd-Warshall multi-query =====

    @Test
    fun `floyd-warshall returns consistent results across repeated queries on the same graph`() {
        val fw = FloydWarshallPathFindingStrategy()
        val first = fw.findPath(noArbitrageGraph, usd, jpy)!!
        val second = fw.findPath(noArbitrageGraph, gbp, jpy)!!
        // We can't directly observe "no recompute" from the outside without
        // timing hacks; the contract under test is correctness across multiple
        // queries on the same graph instance.
        assertEquals(2, first.hops)
        assertEquals(1, second.hops)
        assertEquals(190.0, second.compositeRate, 1e-12)
    }

    // ===== StrategyChain =====

    @Test
    fun `strategy chain returns the first non-null result`() {
        val chain = StrategyChain(
            listOf(
                DirectLookupStrategy(), // null for USD->JPY (no direct edge)
                BfsPathFindingStrategy(), // finds 2 hops
            ),
        )
        val path = chain.findPath(noArbitrageGraph, usd, jpy)
        assertNotNull(path)
        assertEquals(2, path!!.hops)
    }

    @Test
    fun `strategy chain short-circuits on the first hit`() {
        val chain = StrategyChain(
            listOf(
                DirectLookupStrategy(), // hits USD->EUR directly
                BfsPathFindingStrategy(), // would also find a path; must not be consulted
            ),
        )
        val path = chain.findPath(noArbitrageGraph, usd, eur)!!
        assertEquals(1, path.hops)
    }
}
