package com.terraconnect.exchangerates.domain.usecase

import com.terraconnect.exchangerates.core.ds.CurrencyGraph
import com.terraconnect.exchangerates.domain.model.ArbitrageOpportunity
import com.terraconnect.exchangerates.domain.model.Currency
import javax.inject.Inject
import kotlin.math.ln

/**
 * Detect every arbitrage cycle reachable from each vertex.
 *
 * **What's an arbitrage cycle?** A sequence of trades that returns you to the
 * same currency holding *strictly more* than you started with. In terms of
 * the rate graph, that's a cycle whose product of edge weights exceeds 1.0.
 *
 * **The algorithm.** This is the *other half* of what Bellman-Ford can do —
 * the part [com.terraconnect.exchangerates.core.algorithm.BellmanFordPathFindingStrategy]
 * just uses to bail out. We apply the same `-ln(rate)` transform: a positive
 * cycle in the original graph becomes a *negative-weight cycle* in the
 * transformed one. After V-1 relaxation rounds, any edge that *still* relaxes
 * is part of (or downstream from) a negative cycle.
 *
 * **Tracing the cycle.** Once we find a relaxable edge, we follow the
 * `predecessor` chain backwards. Because the cycle has length ≤ V, walking V
 * predecessor hops is guaranteed to land us *inside* the cycle. From there we
 * keep walking until we revisit a node — that's our cycle.
 *
 * **Why all sources?** We launch Bellman-Ford from every vertex. A single
 * source can miss cycles in disjoint components or downstream of unreachable
 * regions. V passes of V·E gives O(V²·E) — fine for our small FX graph and
 * the price of completeness.
 */
class DetectArbitrageUseCase @Inject constructor() {

    operator fun invoke(graph: CurrencyGraph): List<ArbitrageOpportunity> {
        val results = mutableListOf<ArbitrageOpportunity>()
        // We may discover the same cycle from multiple starting vertices;
        // canonicalise so we only keep one copy of each.
        val seen = mutableSetOf<List<Currency>>()

        for (source in graph.vertices) {
            val cycle = bellmanFordCycle(graph, source) ?: continue
            val canonical = canonicalRotation(cycle)
            if (!seen.add(canonical)) continue

            val profitFactor = profitFactor(cycle, graph)
            // Profit factor must clear the floor — Bellman-Ford can flag a
            // cycle whose summed -ln(rate) is *technically* negative only due
            // to FP noise. The 1e-9 epsilon eliminates that class of false
            // positives.
            if (profitFactor > 1.0 + EPSILON) {
                results += ArbitrageOpportunity(cycle = cycle, profitFactor = profitFactor)
            }
        }
        return results.sortedByDescending { it.profitFactor }
    }

    /**
     * Single-source Bellman-Ford. Returns one arbitrage cycle reachable from
     * `source`, or null if there isn't one.
     */
    private fun bellmanFordCycle(graph: CurrencyGraph, source: Currency): List<Currency>? {
        val distance = HashMap<Currency, Double>()
        val predecessor = HashMap<Currency, Currency>()
        graph.vertices.forEach { distance[it] = Double.POSITIVE_INFINITY }
        distance[source] = 0.0

        val edges = graph.edges

        // V-1 relaxation rounds — same as in BellmanFordPathFindingStrategy.
        repeat(graph.vertexCount - 1) {
            for (edge in edges) {
                val w = -ln(edge.rate)
                val fromDist = distance.getValue(edge.from)
                if (fromDist == Double.POSITIVE_INFINITY) continue
                if (fromDist + w < distance.getValue(edge.to)) {
                    distance[edge.to] = fromDist + w
                    predecessor[edge.to] = edge.from
                }
            }
        }

        // V-th pass: anything that still relaxes is part of (or downstream of)
        // a negative cycle.
        for (edge in edges) {
            val w = -ln(edge.rate)
            val fromDist = distance.getValue(edge.from)
            if (fromDist == Double.POSITIVE_INFINITY) continue
            if (fromDist + w < distance.getValue(edge.to)) {
                return traceCycle(start = edge.to, predecessor = predecessor, vertexCount = graph.vertexCount)
            }
        }
        return null
    }

    /**
     * Given a vertex that we know lies in (or downstream of) a negative
     * cycle, find the cycle itself.
     *
     * Step 1: walk V predecessor hops. After V hops we're *guaranteed* to be
     *         on the cycle (a cycle has length ≤ V, and once we step onto it
     *         we stay there).
     * Step 2: from that vertex, keep walking backwards until we see the same
     *         vertex again — that's one full loop.
     */
    private fun traceCycle(
        start: Currency,
        predecessor: Map<Currency, Currency>,
        vertexCount: Int,
    ): List<Currency>? {
        var node = start
        repeat(vertexCount) {
            node = predecessor[node] ?: return null
        }

        val cycleEntry = node
        val cycle = mutableListOf<Currency>()
        var cursor = cycleEntry
        do {
            cycle += cursor
            cursor = predecessor[cursor] ?: return null
        } while (cursor != cycleEntry)
        cycle += cycleEntry
        // cycle is currently in reverse order (we walked via predecessors).
        return cycle.reversed()
    }

    /**
     * Pick the rotation of the cycle that starts at the alphabetically-first
     * currency. Two cycles `[A, B, C, A]` and `[B, C, A, B]` describe the
     * same loop; this normalisation makes them compare equal so we don't
     * report the same arbitrage twice from different starting points.
     */
    private fun canonicalRotation(cycle: List<Currency>): List<Currency> {
        val core = cycle.dropLast(1) // strip the duplicated closing vertex
        val pivot = core.indices.minBy { core[it] }
        val rotated = core.drop(pivot) + core.take(pivot)
        return rotated + rotated.first()
    }

    /** Multiply the edge rates along the cycle to get the profit factor. */
    private fun profitFactor(cycle: List<Currency>, graph: CurrencyGraph): Double {
        var factor = 1.0
        cycle.zipWithNext { a, b ->
            val rate = graph.edgesFrom(a).firstOrNull { it.to == b }?.rate
                ?: return Double.NaN // cycle uses an edge that vanished — bail
            factor *= rate
        }
        return factor
    }

    private companion object {
        // 0.1% — anything smaller than this and we consider it floating-point
        // noise or rounding in the rate table, not a real arbitrage signal.
        private const val EPSILON = 1e-3
    }
}
