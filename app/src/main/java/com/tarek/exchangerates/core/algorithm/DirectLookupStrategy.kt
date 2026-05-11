package com.tarek.exchangerates.core.algorithm

import com.tarek.exchangerates.core.ds.CurrencyGraph
import com.tarek.exchangerates.domain.model.ConversionPath
import com.tarek.exchangerates.domain.model.Currency
import com.tarek.exchangerates.domain.model.ExchangeRate
import javax.inject.Inject

/**
 * O(1) hash-map probe — useful as the head of a [StrategyChain] so callers
 * never pay BFS/Dijkstra overhead when the direct edge is already there.
 */
class DirectLookupStrategy @Inject constructor() :
    AbstractPathFindingStrategy(StrategyKind.DIRECT) {

    override fun search(graph: CurrencyGraph, from: Currency, to: Currency): ConversionPath? {
        val rate = graph.directRate(from, to) ?: return null
        return ConversionPath(source = from, edges = listOf(ExchangeRate(from, to, rate)))
    }
}
