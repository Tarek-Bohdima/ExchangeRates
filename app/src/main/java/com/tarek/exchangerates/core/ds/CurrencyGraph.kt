package com.tarek.exchangerates.core.ds

import com.tarek.exchangerates.domain.model.Currency
import com.tarek.exchangerates.domain.model.ExchangeRate

/**
 * Currency graph — the data structure every algorithm in this app operates on.
 *
 * **The model.** A *directed weighted multigraph*:
 *  * directed:    an edge "USD → EUR @ 0.92" is *not* the same as "EUR → USD @ 1.087"
 *                 (in real markets bid and ask aren't symmetric);
 *  * weighted:    each edge carries a rate, the multiplier you'd apply when converting;
 *  * multigraph:  in principle nothing stops two edges between the same pair
 *                 (e.g. two brokers quoting the same pair) — we just keep them all.
 *
 * **Representation: adjacency map.** A `Map<Currency, List<ExchangeRate>>`
 * keyed by the source vertex. Two representations to choose from:
 *
 *  * Adjacency *matrix* — a V×V grid of weights. Excellent if you ask "what's
 *    the direct rate between i and j?" in a tight loop, awful for memory when
 *    V is hundreds and most cells are empty.
 *  * Adjacency *list/map* — for each vertex, the list of edges that *leave*
 *    it. Great memory profile and great for "iterate the neighbours" — which
 *    is exactly what BFS, Dijkstra, and Bellman-Ford all need.
 *
 * FX graphs are sparse (most currency pairs aren't directly quoted) and the
 * algorithms care about "neighbours of X" far more than "is there an edge X→Y",
 * so the map wins.
 *
 * **Construction goes through [Builder]** (GoF Builder pattern). The builder
 * enforces every invariant *during construction*; once you hold a CurrencyGraph
 * you're guaranteed the data inside is well-formed.
 */
class CurrencyGraph private constructor(
    private val adjacency: Map<Currency, List<ExchangeRate>>,
) {

    val vertices: Set<Currency> = adjacency.keys
    val edges: List<ExchangeRate> = adjacency.values.flatten()
    val vertexCount: Int get() = vertices.size
    val edgeCount: Int get() = edges.size

    fun contains(currency: Currency): Boolean = currency in adjacency

    /** O(1) — return the cached outgoing edge list for a vertex. */
    fun edgesFrom(currency: Currency): List<ExchangeRate> =
        adjacency[currency].orEmpty()

    /**
     * Direct rate lookup with no path-finding: scan the (small) outgoing-edge
     * list of `from` for an edge whose destination is `to`. O(out-degree),
     * which is tiny in a real FX graph (a handful of edges per currency).
     * Returns null when no direct edge exists.
     */
    fun directRate(from: Currency, to: Currency): Double? =
        adjacency[from]?.firstOrNull { it.to == to }?.rate

    override fun toString(): String =
        "CurrencyGraph(V=$vertexCount, E=$edgeCount)"

    /**
     * Builder (GoF) — fluent, validating, single-shot.
     *
     * Two reasons we don't expose a raw constructor:
     *  1. We want to *normalise* the input (e.g. expand reciprocal edges in
     *     one place);
     *  2. We want every CurrencyGraph instance to be immutable — the builder
     *     materialises an immutable snapshot in [build].
     */
    class Builder {
        private val edges = mutableListOf<ExchangeRate>()
        private var addReciprocals = false

        fun addRate(rate: ExchangeRate): Builder = apply { edges += rate }

        fun addRates(rates: Iterable<ExchangeRate>): Builder = apply { edges += rates }

        /**
         * If enabled, each added edge `from→to @ r` automatically generates
         * its inverse `to→from @ 1/r`. Most fake datasets only quote one
         * direction; flipping this on simulates a symmetric market.
         */
        fun withReciprocals(enabled: Boolean = true): Builder = apply {
            addReciprocals = enabled
        }

        fun build(): CurrencyGraph {
            val finalEdges = if (addReciprocals) {
                edges.flatMap { listOf(it, it.inverse()) }
            } else {
                edges.toList()
            }
            // Build the adjacency map in one pass. The second `getOrPut` on
            // `edge.to` is what makes sink-only vertices (no outgoing edges)
            // still show up in `vertices` — otherwise BFS would mistakenly
            // think they don't exist.
            val adjacency = buildMap<Currency, MutableList<ExchangeRate>> {
                finalEdges.forEach { edge ->
                    getOrPut(edge.from) { mutableListOf() } += edge
                    getOrPut(edge.to) { mutableListOf() }
                }
            }.mapValues { (_, v) -> v.toList() }
            return CurrencyGraph(adjacency)
        }
    }

    companion object {
        fun builder(): Builder = Builder()
    }
}
