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
 * Behaviour parity test for every strategy.
 *
 * On an arbitrage-free graph, BFS may pick a different *path* than Dijkstra
 * (it minimises hops, not rate), but Dijkstra, Bellman-Ford, and
 * Floyd-Warshall must all agree on the *best composite rate*. That single
 * invariant is enough to catch the most common algorithm bugs in one assertion.
 */
class PathFindingStrategyTest {

    private val usd = Currency.of("USD")
    private val eur = Currency.of("EUR")
    private val gbp = Currency.of("GBP")
    private val jpy = Currency.of("JPY")

    private val graph = CurrencyGraph.builder()
        .addRate(ExchangeRate(usd, eur, 0.90))   // direct
        .addRate(ExchangeRate(usd, gbp, 0.78))
        .addRate(ExchangeRate(eur, gbp, 0.85))
        .addRate(ExchangeRate(gbp, jpy, 190.0))
        .addRate(ExchangeRate(eur, jpy, 165.0))
        .build()

    @Test
    fun `direct lookup returns single edge when present`() {
        val path = DirectLookupStrategy().findPath(graph, usd, eur)
        assertNotNull(path)
        assertEquals(1, path!!.hops)
        assertEquals(0.90, path.compositeRate, 1e-9)
    }

    @Test
    fun `direct lookup returns null when no direct edge`() {
        assertNull(DirectLookupStrategy().findPath(graph, usd, jpy))
    }

    @Test
    fun `bfs returns shortest hop path`() {
        // USD -> JPY: direct edge doesn't exist; BFS should pick USD->GBP->JPY
        // or USD->EUR->JPY (both 2 hops). Either is acceptable as long as it's
        // 2 hops, not 1 (no direct) and not 3.
        val path = BfsPathFindingStrategy().findPath(graph, usd, jpy)
        assertNotNull(path)
        assertEquals(2, path!!.hops)
    }

    @Test
    fun `dijkstra and bellman-ford agree on best rate`() {
        val dijkstraPath = DijkstraPathFindingStrategy().findPath(graph, usd, jpy)
        val bfPath = BellmanFordPathFindingStrategy().findPath(graph, usd, jpy)
        assertNotNull(dijkstraPath)
        assertNotNull(bfPath)
        assertEquals(dijkstraPath!!.compositeRate, bfPath!!.compositeRate, 1e-9)
    }

    @Test
    fun `floyd-warshall agrees with dijkstra on best rate`() {
        val dijkstraPath = DijkstraPathFindingStrategy().findPath(graph, usd, jpy)
        val fwPath = FloydWarshallPathFindingStrategy().findPath(graph, usd, jpy)
        assertNotNull(dijkstraPath)
        assertNotNull(fwPath)
        assertEquals(dijkstraPath!!.compositeRate, fwPath!!.compositeRate, 1e-9)
    }

    @Test
    fun `unreachable target returns null`() {
        val xyz = Currency.of("XYZ")
        val isolatedGraph = CurrencyGraph.builder()
            .addRate(ExchangeRate(usd, eur, 0.9))
            .addRate(ExchangeRate(jpy, gbp, 0.005))
            .build()
        // USD and JPY are in different components — no path possible.
        assertNull(BfsPathFindingStrategy().findPath(isolatedGraph, usd, gbp))
        // XYZ doesn't even exist in the graph.
        assertTrue(isolatedGraph.contains(usd))
        assertTrue(!isolatedGraph.contains(xyz))
    }
}
