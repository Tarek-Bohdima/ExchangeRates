package com.terraconnect.exchangerates.core.algorithm

import com.terraconnect.exchangerates.core.ds.CurrencyGraph
import com.terraconnect.exchangerates.domain.model.ConversionPath
import com.terraconnect.exchangerates.domain.model.Currency
import com.terraconnect.exchangerates.domain.model.ExchangeRate
import javax.inject.Inject
import kotlin.math.ln

/**
 * Bellman-Ford — the FX-friendly path finder.
 *
 * **The clever bit: the `-ln(rate)` transform.**
 * We want to *maximise* the product of rates along a path. But every
 * shortest-path algorithm in the textbook *minimises* a sum. So we trade
 * problems: take the natural log of every rate. Now the product
 * `r₁ · r₂ · r₃` becomes the sum `ln(r₁) + ln(r₂) + ln(r₃)`. Maximising one
 * maximises the other (ln is monotonically increasing). Finally, flip the
 * sign — `-ln(rate)` — and now *minimising* the sum maximises the original
 * product. We just turned a max-product problem into a classic shortest-path
 * problem.
 *
 * **Why not Dijkstra after the transform?**
 * Because rates `> 1` produce *negative* `-ln(rate)` edges, and Dijkstra
 * cannot handle negative weights. Bellman-Ford can.
 *
 * **The algorithm in one line:** "relax every edge V-1 times".
 * "Relaxing" `u -> v` means: *if going through u gives v a shorter distance,
 * update it*. After V-1 passes, every simple path of up to V-1 edges has been
 * considered — and any shortest path in a graph of V vertices has at most
 * V-1 edges (more would imply a cycle, which can never help).
 *
 * **Arbitrage detection — the V-th pass.**
 * After V-1 passes the answer is final *unless* a negative-weight cycle
 * exists. If a V-th relaxation pass *still* finds an improvement, that
 * improvement can only come from looping around a negative cycle — which, in
 * the original (untransformed) graph, is a sequence of trades whose product
 * of rates exceeds 1.0. That's arbitrage. When we detect it, we bail out
 * (the "best path" answer would be unreliable because we could just loop the
 * cycle indefinitely for ever-better profit).
 *
 * Complexity: O(V · E) time, O(V) extra space.
 */
class BellmanFordPathFindingStrategy @Inject constructor() :
    AbstractPathFindingStrategy(StrategyKind.BELLMAN_FORD) {

    override fun search(graph: CurrencyGraph, from: Currency, to: Currency): ConversionPath? {
        // `distance[v]` = best `-ln(rate)` sum we've seen for a path source→v.
        // Initialised to +∞ ("unknown") for every vertex; 0 for the source
        // (zero-edge path has zero summed weight).
        val distance = HashMap<Currency, Double>().apply {
            graph.vertices.forEach { put(it, Double.POSITIVE_INFINITY) }
            put(from, 0.0)
        }
        // Same breadcrumb idea as BFS/Dijkstra.
        val cameFrom = HashMap<Currency, ExchangeRate>()
        val edges = graph.edges

        // -------- Main loop: at most V-1 relaxation rounds. --------
        // Why V-1? A path through a graph with V vertices has at most V-1
        // edges (any more requires repeating a vertex, i.e. a cycle, and
        // shortest paths are never cyclic in a graph without negative cycles).
        repeat(graph.vertexCount - 1) {
            var changed = false
            for (edge in edges) {
                val weight = -ln(edge.rate) // The transform: turn rate into a weight we can sum.
                val fromDist = distance.getValue(edge.from)
                if (fromDist == Double.POSITIVE_INFINITY) continue // unreachable yet — nothing to relax through

                val candidate = fromDist + weight
                if (candidate < distance.getValue(edge.to)) {
                    distance[edge.to] = candidate
                    cameFrom[edge.to] = edge
                    changed = true
                }
            }
            // Tiny optimisation: if a full pass did nothing, all remaining
            // passes will also do nothing. We're done early.
            if (!changed) return@repeat
        }

        // -------- The V-th pass: arbitrage detection. --------
        // After V-1 passes everything legitimate has settled. If *any* edge
        // still relaxes here, the only possible reason is a negative cycle —
        // arbitrage in the original graph. The "best path" question is then
        // ill-defined (you could just loop the cycle), so we abort.
        for (edge in edges) {
            val weight = -ln(edge.rate)
            val fromDist = distance.getValue(edge.from)
            if (fromDist == Double.POSITIVE_INFINITY) continue
            if (fromDist + weight < distance.getValue(edge.to)) return null
        }

        // No path reached the target at all — different components.
        if (distance.getValue(to) == Double.POSITIVE_INFINITY) return null
        return reconstruct(cameFrom, from, to)
    }

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
