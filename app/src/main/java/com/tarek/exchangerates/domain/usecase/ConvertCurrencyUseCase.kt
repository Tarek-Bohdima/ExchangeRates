package com.tarek.exchangerates.domain.usecase

import com.tarek.exchangerates.core.algorithm.StrategyFactory
import com.tarek.exchangerates.core.algorithm.StrategyKind
import com.tarek.exchangerates.core.ds.CurrencyGraph
import com.tarek.exchangerates.domain.model.ConversionPath
import com.tarek.exchangerates.domain.model.ConversionResult
import com.tarek.exchangerates.domain.model.Currency
import com.tarek.exchangerates.domain.model.Money
import javax.inject.Inject

/**
 * The "convert this amount, please" operation.
 *
 * Single responsibility: given a graph, an input [Money], a target currency,
 * and a chosen algorithm, **delegate** the search to a [StrategyFactory] and
 * shape the result into a [ConversionResult] the UI can `when`-switch on.
 *
 * **Law of Demeter at work.** This class only talks to its direct
 * collaborators (`strategyFactory`, `graph`, model types). It never reaches
 * *through* one to get to another — e.g. it calls `factory.get(kind)`, not
 * `factory.strategies[kind]?.findPath(...)`. That makes the class trivially
 * testable: stub the factory, control the strategy, assert on the result.
 */
class ConvertCurrencyUseCase @Inject constructor(
    private val strategyFactory: StrategyFactory,
) {

    operator fun invoke(
        graph: CurrencyGraph,
        input: Money,
        target: Currency,
        kind: StrategyKind = StrategyKind.DIJKSTRA,
    ): ConversionResult {
        // Guard: both endpoints must be vertices of the graph.
        if (!graph.contains(input.currency)) return ConversionResult.UnknownCurrency(input.currency)
        if (!graph.contains(target)) return ConversionResult.UnknownCurrency(target)

        // Identity conversion — no graph search needed. The empty-path branch
        // of ConversionPath models this cleanly and lets us keep ExchangeRate's
        // "no self-edges" invariant intact.
        if (input.currency == target) {
            return ConversionResult.Success(
                path = ConversionPath(source = input.currency, edges = emptyList()),
                output = input,
            )
        }

        val path = strategyFactory.get(kind).findPath(graph, input.currency, target)
            ?: return ConversionResult.Unreachable(input.currency, target)

        val output = input.convertedTo(target = target, rate = path.compositeRate)
        return ConversionResult.Success(path = path, output = output)
    }
}
