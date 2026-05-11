package com.tarek.exchangerates.domain.usecase

import com.tarek.exchangerates.core.algorithm.StrategyKind
import com.tarek.exchangerates.core.ds.CurrencyGraph
import com.tarek.exchangerates.data.repository.ExchangeRatesRepository
import com.tarek.exchangerates.domain.model.ArbitrageOpportunity
import com.tarek.exchangerates.domain.model.ConversionResult
import com.tarek.exchangerates.domain.model.Currency
import com.tarek.exchangerates.domain.model.Money
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/**
 * Facade (GoF) over the three use cases plus the repository.
 *
 * Exposed as an interface so the ViewModel depends on the abstraction, not the
 * concrete coordination class. That's the whole point of "code to abstractions":
 * the consumer needs only the contract, and tests can substitute a fake facade
 * without spinning up the real use-case graph.
 *
 * The default implementation is purely a coordination layer — the actual work
 * still lives in the individual use cases, so it's not a god-class.
 */
interface ConversionFacade {
    fun observeGraph(): Flow<CurrencyGraph>
    suspend fun refresh()
    fun convert(graph: CurrencyGraph, input: Money, target: Currency, kind: StrategyKind): ConversionResult
    fun arbitrage(graph: CurrencyGraph): List<ArbitrageOpportunity>
    fun reachableFrom(graph: CurrencyGraph, source: Currency): Set<Currency>
}

class DefaultConversionFacade @Inject constructor(
    private val repository: ExchangeRatesRepository,
    private val convertCurrency: ConvertCurrencyUseCase,
    private val detectArbitrageUseCase: DetectArbitrageUseCase,
    private val reachableCurrencies: GetReachableCurrenciesUseCase,
) : ConversionFacade {

    override fun observeGraph(): Flow<CurrencyGraph> = repository.observeGraph()

    override suspend fun refresh() = repository.refresh()

    override fun convert(
        graph: CurrencyGraph,
        input: Money,
        target: Currency,
        kind: StrategyKind,
    ): ConversionResult = convertCurrency(graph, input, target, kind)

    override fun arbitrage(graph: CurrencyGraph): List<ArbitrageOpportunity> =
        detectArbitrageUseCase(graph)

    override fun reachableFrom(graph: CurrencyGraph, source: Currency): Set<Currency> =
        reachableCurrencies(graph, source)
}
