package com.tarek.exchangerates.core.algorithm

import com.tarek.exchangerates.core.ds.CurrencyGraph
import com.tarek.exchangerates.domain.model.ConversionPath
import com.tarek.exchangerates.domain.model.Currency
import com.tarek.exchangerates.domain.model.ExchangeRate
import java.util.ArrayDeque
import javax.inject.Inject

/**
 * BFS — Breadth-First Search.
 *
 * The intuition: think of the graph as ripples spreading out from the source.
 * In ripple 1 we visit every currency reachable in 1 hop, in ripple 2 every
 * currency reachable in 2 hops, and so on. The moment a ripple reaches the
 * target, we *know* it took the minimum possible number of hops — because no
 * earlier ripple was wide enough to include it.
 *
 * BFS ignores rate quality entirely: every edge counts as 1. So a 1-hop path
 * with a terrible rate still beats a 5-hop path with a great one. Use this
 * when "fewest conversions" is the optimisation goal, or as a cheap
 * reachability probe before reaching for Dijkstra.
 *
 * Complexity: O(V + E) time, O(V) auxiliary space.
 */
class BfsPathFindingStrategy @Inject constructor() :
    AbstractPathFindingStrategy(StrategyKind.BFS) {

    override fun search(graph: CurrencyGraph, from: Currency, to: Currency): ConversionPath? {
        // `cameFrom[v]` records *which edge we used to first reach v*. Together
        // these form a tree we can walk backwards from `to` to recover the path.
        val cameFrom = HashMap<Currency, ExchangeRate>()

        // FIFO queue is the engine of BFS: we always expand the *oldest* vertex
        // we've discovered, which is what enforces the layer-by-layer ordering.
        val queue = ArrayDeque<Currency>().apply { offer(from) }

        // Mark `from` as visited up front so we never re-enqueue it.
        val visited = HashSet<Currency>().apply { add(from) }

        while (queue.isNotEmpty()) {
            val current = queue.poll()

            // Found the target — its `cameFrom` chain is the shortest-hop path.
            if (current == to) return reconstruct(cameFrom, source = from, target = to)

            // Expand: look at every neighbour of `current`.
            graph.edgesFrom(current).forEach { edge ->
                // `visited.add(...)` returns false if it was already there, so
                // this one call both checks and marks in a single step.
                if (visited.add(edge.to)) {
                    cameFrom[edge.to] = edge
                    queue.offer(edge.to)
                }
            }
        }
        // Queue drained without ever reaching `to` — they're in different
        // connected components.
        return null
    }

    /**
     * Walk the `cameFrom` tree from `target` back to the source, collecting
     * edges. We push to the *front* so the final list is in forward order.
     */
    private fun reconstruct(
        cameFrom: Map<Currency, ExchangeRate>,
        source: Currency,
        target: Currency,
    ): ConversionPath {
        val reversed = ArrayDeque<ExchangeRate>()
        var node = target
        while (true) {
            val edge = cameFrom[node] ?: break
            reversed.addFirst(edge)
            node = edge.from
        }
        return ConversionPath(source = source, edges = reversed.toList())
    }
}
