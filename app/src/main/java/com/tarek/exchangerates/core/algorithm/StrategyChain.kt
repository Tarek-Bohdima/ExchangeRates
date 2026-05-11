package com.tarek.exchangerates.core.algorithm

import com.tarek.exchangerates.core.ds.CurrencyGraph
import com.tarek.exchangerates.domain.model.ConversionPath
import com.tarek.exchangerates.domain.model.Currency

/**
 * Chain of Responsibility (GoF) over [PathFindingStrategy].
 *
 * Wraps an ordered list of strategies and returns the first non-null result.
 * The intended composition is *cheap-first*:
 *
 * ```
 * Direct lookup  → BFS  → Dijkstra  → Bellman-Ford
 *      O(1)        O(V+E)   O(V+E·logV)   O(V·E)
 * ```
 *
 * If the direct edge exists the chain stops there and never touches the heap;
 * if BFS finds anything we never run Dijkstra, etc. This is the same pattern
 * the JDK uses for `URLStreamHandlerFactory` lookups — try the registered
 * handlers in order, stop at the first hit.
 */
class StrategyChain(private val strategies: List<PathFindingStrategy>) : PathFindingStrategy {

    init {
        require(strategies.isNotEmpty()) { "StrategyChain needs at least one strategy" }
    }

    override fun findPath(
        graph: CurrencyGraph,
        from: Currency,
        to: Currency,
    ): ConversionPath? {
        for (strategy in strategies) {
            strategy.findPath(graph, from, to)?.let { return it }
        }
        return null
    }
}
