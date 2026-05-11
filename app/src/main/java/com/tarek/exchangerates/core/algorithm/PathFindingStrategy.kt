package com.tarek.exchangerates.core.algorithm

import com.tarek.exchangerates.core.ds.CurrencyGraph
import com.tarek.exchangerates.domain.model.ConversionPath
import com.tarek.exchangerates.domain.model.Currency

/**
 * Strategy (GoF) interface for finding a conversion path between two currencies.
 *
 * Implementations differ in algorithm, complexity, and which property of the
 * path they optimise — fewest hops, best composite rate, arbitrage tolerance,
 * etc. Returning `null` means "no path exists from [from] to [to] in [graph]".
 *
 * Same-currency calls are *not* the strategy's concern: the use case layer
 * filters them out beforehand because they have no meaningful edges to model.
 */
fun interface PathFindingStrategy {
    fun findPath(graph: CurrencyGraph, from: Currency, to: Currency): ConversionPath?
}
