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
 * The ViewModel could inject each use case individually, but then it would
 * know about four collaborators and the order in which to chain them. The
 * Facade hides that subsystem behind one cohesive object: "convert", "scan
 * for arbitrage", "what's reachable". The ViewModel's surface shrinks; the
 * Law of Demeter stays happy; tests get a single seam to substitute.
 *
 * Not a god-class — the actual work still lives in the individual use cases.
 * This is purely a coordination layer.
 */
class ConversionFacade @Inject constructor(
    private val repository: ExchangeRatesRepository,
    private val convertCurrency: ConvertCurrencyUseCase,
    private val detectArbitrageUseCase: DetectArbitrageUseCase,
    private val reachableCurrencies: GetReachableCurrenciesUseCase,
) {

    fun observeGraph(): Flow<CurrencyGraph> = repository.observeGraph()

    suspend fun refresh() = repository.refresh()

    fun convert(
        graph: CurrencyGraph,
        input: Money,
        target: Currency,
        kind: StrategyKind,
    ): ConversionResult = convertCurrency(graph, input, target, kind)

    fun arbitrage(graph: CurrencyGraph): List<ArbitrageOpportunity> = detectArbitrageUseCase(graph)

    fun reachableFrom(graph: CurrencyGraph, source: Currency): Set<Currency> =
        reachableCurrencies(graph, source)
}
