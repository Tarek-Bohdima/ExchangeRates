package com.terraconnect.exchangerates.core.algorithm

import com.terraconnect.exchangerates.core.ds.CurrencyGraph
import com.terraconnect.exchangerates.domain.model.ConversionPath
import com.terraconnect.exchangerates.domain.model.Currency
import com.terraconnect.exchangerates.domain.model.ExchangeRate
import javax.inject.Inject
import kotlin.math.ln

/**
 * Floyd-Warshall — all-pairs shortest path via dynamic programming.
 *
 * **The trade.** Pay O(V³) once up front, then *every* future conversion
 * query is an O(1) matrix lookup plus path reconstruction. Worth it when the
 * same graph answers many queries (the conversion explorer screen does, hundreds
 * potentially as the user tries different inputs).
 *
 * **The DP idea.** Number the vertices 0..n-1. Define `D[k][i][j]` as the
 * shortest distance from i to j *using only vertices 0..k as intermediates*.
 *
 *   * Base case `D[-1][i][j]` is just the direct edge weight (or +∞).
 *   * Step: `D[k][i][j] = min(D[k-1][i][j], D[k-1][i][k] + D[k-1][k][j])`.
 *     Either k doesn't help (keep the old answer), or going `i → k → j` beats
 *     it.
 *
 * That recurrence collapses into the famous triple loop. The key subtlety:
 * **k must be the *outermost* loop**. That's how we guarantee each `D[k][i][j]`
 * is computed only after every `D[k-1][.][.]` it depends on is already final.
 * Swap the loops around and the algorithm silently breaks.
 *
 * Like Bellman-Ford, this uses `-ln(rate)` weights so the min-sum problem
 * encodes the max-product question.
 *
 * **Caching.** Since the precompute is the expensive part, we memoise the
 * result keyed by graph identity. If the repository emits a brand-new graph
 * (rates refreshed), the next query rebuilds; otherwise we reuse.
 */
class FloydWarshallPathFindingStrategy @Inject constructor() :
    AbstractPathFindingStrategy(StrategyKind.FLOYD_WARSHALL) {

    /** Precomputed tables for a single graph instance. */
    private data class Precomputed(
        val index: Map<Currency, Int>,       // currency → row/col index
        val vertices: List<Currency>,        // reverse mapping
        val dist: Array<DoubleArray>,        // dist[i][j] = best -ln(rate) sum
        val next: Array<IntArray>,           // next[i][j] = next hop on path i→j
        val edge: Array<Array<ExchangeRate?>>, // direct edge i→j, if any
    )

    @Volatile
    private var cached: Pair<CurrencyGraph, Precomputed>? = null

    override fun search(graph: CurrencyGraph, from: Currency, to: Currency): ConversionPath? {
        val pre = precomputeFor(graph)
        val i = pre.index.getValue(from)
        val j = pre.index.getValue(to)
        // -1 in `next[i][j]` is our sentinel for "no path".
        if (pre.next[i][j] == NO_PATH) return null
        if (!pre.dist[i][j].isFinite()) return null
        return reconstruct(pre, i, j)
    }

    private fun precomputeFor(graph: CurrencyGraph): Precomputed {
        // Quick hit: if the cache is keyed by this exact graph instance, reuse it.
        cached?.let { (cachedGraph, pre) -> if (cachedGraph === graph) return pre }

        val vertices = graph.vertices.toList()
        val n = vertices.size
        val index = vertices.withIndex().associate { (idx, v) -> v to idx }

        // dist[i][j] starts as +∞ — "no known path".
        val dist = Array(n) { DoubleArray(n) { Double.POSITIVE_INFINITY } }
        // next[i][j] = -1 means "no path"; otherwise it's the next hop on i→j.
        val next = Array(n) { IntArray(n) { NO_PATH } }
        // Remember the direct edge between two vertices (for cleanly recovering
        // ExchangeRate objects during reconstruction).
        val edge = Array(n) { arrayOfNulls<ExchangeRate>(n) }

        // Base case 1: distance from a vertex to itself is 0.
        for (i in 0 until n) dist[i][i] = 0.0
        // Base case 2: seed with every direct edge. If multiple edges go u→v
        // (multigraph), keep the best.
        for (rate in graph.edges) {
            val u = index.getValue(rate.from)
            val v = index.getValue(rate.to)
            val w = -ln(rate.rate)
            if (w < dist[u][v]) {
                dist[u][v] = w
                next[u][v] = v
                edge[u][v] = rate
            }
        }

        // -------- The triple loop. --------
        // For each candidate intermediate `k` (outermost!), check every pair
        // (i, j) and see whether i → ... → k → ... → j is shorter than the
        // best i → j we've found so far.
        for (k in 0 until n) {
            for (i in 0 until n) {
                if (dist[i][k] == Double.POSITIVE_INFINITY) continue // can't reach k from i, skip
                for (j in 0 until n) {
                    val candidate = dist[i][k] + dist[k][j]
                    if (candidate < dist[i][j]) {
                        dist[i][j] = candidate
                        // Crucial: the next hop on the *new* shorter path is
                        // the same first hop you'd take going from i to k.
                        next[i][j] = next[i][k]
                    }
                }
            }
        }

        val precomputed = Precomputed(index, vertices, dist, next, edge)
        cached = graph to precomputed
        return precomputed
    }

    /**
     * Walk the `next` matrix one hop at a time, collecting the recorded direct
     * edge for each step. Each direct edge was either a seeded base-case edge
     * or, transitively, a sub-path we'd already reduced — so by induction every
     * `edge[current][step]` along the walk is a real graph edge.
     */
    private fun reconstruct(pre: Precomputed, i: Int, j: Int): ConversionPath? {
        val edges = mutableListOf<ExchangeRate>()
        var current = i
        while (current != j) {
            val step = pre.next[current][j]
            if (step == NO_PATH) return null
            edges += pre.edge[current][step] ?: return null
            current = step
        }
        if (edges.isEmpty()) return null
        return ConversionPath(source = pre.vertices[i], edges = edges)
    }

    private companion object {
        private const val NO_PATH = -1
    }
}
