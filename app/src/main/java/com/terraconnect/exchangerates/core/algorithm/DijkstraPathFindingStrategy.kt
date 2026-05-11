package com.terraconnect.exchangerates.core.algorithm

import com.terraconnect.exchangerates.core.ds.CurrencyGraph
import com.terraconnect.exchangerates.domain.model.ConversionPath
import com.terraconnect.exchangerates.domain.model.Currency
import com.terraconnect.exchangerates.domain.model.ExchangeRate
import java.util.PriorityQueue
import javax.inject.Inject

/**
 * Dijkstra's algorithm — max-product variant for FX rates.
 *
 * **The idea, in one paragraph.** Classic Dijkstra finds the *shortest* path
 * by always expanding the vertex with the smallest distance-so-far. We want
 * the *best* composite exchange rate, which is the largest *product* of edge
 * rates. So instead of a min-heap of distances, we use a max-heap of products,
 * and "relax" an edge `u -> v` whenever `bestProduct[u] * rate(u,v)` beats the
 * best product we'd previously recorded for v.
 *
 * **Why it's faster than Bellman-Ford.** Each vertex is settled at most once,
 * giving the famous O((V+E) log V) bound — much friendlier than O(V·E) when
 * E approaches V². The price you pay is a precondition (see below).
 *
 * **The precondition: no arbitrage.** Dijkstra assumes that once a vertex's
 * best score is "settled" we can never improve it later. With exchange rates
 * that's only true when there's no positive-product cycle — i.e. no arbitrage
 * loop. If arbitrage exists, this still terminates (the lazy-deletion check
 * blocks infinite re-enqueueing) but the answer is not guaranteed optimal. For
 * those graphs use [BellmanFordPathFindingStrategy], which also reports the
 * arbitrage cycle itself.
 */
class DijkstraPathFindingStrategy @Inject constructor() :
    AbstractPathFindingStrategy(StrategyKind.DIJKSTRA) {

    /** A vertex plus the best product we've found for it so far. */
    private data class HeapNode(val vertex: Currency, val product: Double)

    override fun search(graph: CurrencyGraph, from: Currency, to: Currency): ConversionPath? {
        // `best[v]` = greatest product of rates we've seen on any path from
        // `from` to `v`. Source starts at 1.0 (the identity for multiplication).
        val best = HashMap<Currency, Double>().apply { put(from, 1.0) }

        // Reconstruction breadcrumbs: which edge first reached each vertex
        // along the *current* best-known path.
        val cameFrom = HashMap<Currency, ExchangeRate>()

        // Max-heap: PriorityQueue is min-heap by default, so we reverse the
        // comparator. The vertex with the largest product is always next.
        val heap = PriorityQueue<HeapNode>(compareByDescending { it.product })
        heap.offer(HeapNode(from, 1.0))

        while (heap.isNotEmpty()) {
            val (current, currentProduct) = heap.poll()

            // Lazy deletion: we add a *new* heap entry every time a vertex
            // improves rather than mutating the existing one (no decrease-key
            // in Java's PriorityQueue). When we pop, the entry might be stale
            // — its product was already beaten by a later push. Just skip it.
            if (currentProduct < (best[current] ?: Double.NEGATIVE_INFINITY)) continue

            // Greedy short-circuit: a max-heap pops in decreasing order of
            // product, so the first time we pop `to` we already hold its best
            // achievable product. Stop here.
            if (current == to) return reconstruct(cameFrom, from, to)

            // Relaxation: try to improve each neighbour's best product through
            // the current vertex.
            graph.edgesFrom(current).forEach { edge ->
                val tentative = currentProduct * edge.rate
                val previousBest = best[edge.to] ?: Double.NEGATIVE_INFINITY
                if (tentative > previousBest) {
                    best[edge.to] = tentative
                    cameFrom[edge.to] = edge
                    heap.offer(HeapNode(edge.to, tentative))
                }
            }
        }
        // Heap drained without reaching `to` — no path exists.
        return null
    }

    /**
     * Same reconstruction pattern as BFS: walk the breadcrumb map backwards,
     * inserting each edge at the front so the final list reads source→target.
     */
    private fun reconstruct(
        cameFrom: Map<Currency, ExchangeRate>,
        source: Currency,
        target: Currency,
    ): ConversionPath? {
        val edges = ArrayDeque<ExchangeRate>()
        var node = target
        while (node != source) {
            val edge = cameFrom[node] ?: return null
            edges.addFirst(edge)
            node = edge.from
        }
        return if (edges.isEmpty()) null else ConversionPath(source = source, edges = edges.toList())
    }
}
