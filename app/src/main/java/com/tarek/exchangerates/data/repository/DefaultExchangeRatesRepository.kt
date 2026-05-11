package com.tarek.exchangerates.data.repository

import com.tarek.exchangerates.core.ds.CurrencyGraph
import com.tarek.exchangerates.data.source.ExchangeRatesDataSource
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.onSubscription
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Default repository — pulls raw edges from a [ExchangeRatesDataSource] and
 * folds them into an immutable [CurrencyGraph] via the Builder.
 *
 * Reactive shape: `MutableStateFlow<CurrencyGraph?>` holds the latest graph.
 * Null until the first successful fetch, so collectors can `.filterNotNull()`
 * and treat the first emission as "rates are ready".
 *
 * Concurrency: [refresh] is guarded by a [Mutex]; a burst of refresh calls
 * collapses to one in-flight fetch instead of stamping on each other.
 */
@Singleton
class DefaultExchangeRatesRepository @Inject constructor(
    private val dataSource: ExchangeRatesDataSource,
) : ExchangeRatesRepository {

    private val refreshLock = Mutex()
    private val graphState = MutableStateFlow<CurrencyGraph?>(null)

    override fun observeGraph(): Flow<CurrencyGraph> = graphState
        // `onSubscription` runs *once per collector* before any emission.
        // First collector boots the initial fetch lazily; later subscribers
        // get the already-cached value with no rework.
        .onSubscription {
            if (graphState.value == null) refresh()
        }
        .filterNotNull()

    override suspend fun refresh() = refreshLock.withLock {
        val edges = dataSource.fetchRates()
        graphState.value = CurrencyGraph.builder()
            .addRates(edges)
            .build()
    }
}
