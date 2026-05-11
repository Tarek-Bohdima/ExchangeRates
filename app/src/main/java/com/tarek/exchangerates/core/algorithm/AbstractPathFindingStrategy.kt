package com.tarek.exchangerates.core.algorithm

import com.tarek.exchangerates.core.ds.CurrencyGraph
import com.tarek.exchangerates.domain.model.ConversionPath
import com.tarek.exchangerates.domain.model.Currency

/**
 * Template Method (GoF) base for all path-finding strategies.
 *
 * Centralises the boring guard clauses (unknown vertex, identical endpoints) so
 * each concrete algorithm only has to express *its* idea — search, relaxation,
 * matrix update — in [search]. The `final override` on [findPath] keeps that
 * skeleton from being accidentally bypassed by a subclass.
 */
abstract class AbstractPathFindingStrategy(val kind: StrategyKind) : PathFindingStrategy {

    final override fun findPath(
        graph: CurrencyGraph,
        from: Currency,
        to: Currency,
    ): ConversionPath? {
        if (from == to) return null
        if (!graph.contains(from) || !graph.contains(to)) return null
        if (graph.edgesFrom(from).isEmpty()) return null
        return search(graph, from, to)
    }

    protected abstract fun search(
        graph: CurrencyGraph,
        from: Currency,
        to: Currency,
    ): ConversionPath?
}
