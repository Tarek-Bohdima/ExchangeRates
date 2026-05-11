package com.terraconnect.exchangerates.data.repository

import com.terraconnect.exchangerates.core.ds.CurrencyGraph
import com.terraconnect.exchangerates.core.ds.LruCache
import com.terraconnect.exchangerates.domain.model.Currency
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach

/**
 * Decorator (GoF) over an [ExchangeRatesRepository].
 *
 * The Decorator pattern in one paragraph: wrap an object in another object
 * that *implements the same interface* and *delegates to it*, layering extra
 * behaviour on top. Callers can't tell the difference — they're still talking
 * to "an ExchangeRatesRepository".
 *
 * Here the extra behaviour is a side-channel cache of "best direct rate for
 * (from, to)". The wrapped repository still does all the real work; we just
 * keep an [LruCache] of recently-asked direct rates and invalidate it on
 * every graph refresh so we never serve stale data.
 *
 * The interesting design point: this decorator could just as easily wrap
 * itself in *another* decorator — say, a `LoggingExchangeRatesRepository` —
 * which is the whole reason Decorator beats subclassing. Composition over
 * inheritance, made literal.
 */
class CachingExchangeRatesRepository(
    private val delegate: ExchangeRatesRepository,
    private val cacheCapacity: Int = DEFAULT_CAPACITY,
) : ExchangeRatesRepository by delegate {

    private val cache = LruCache<Pair<Currency, Currency>, Double>(cacheCapacity)

    override fun observeGraph(): Flow<CurrencyGraph> =
        // Every time a new graph arrives the cache is stale by definition —
        // currencies might have appeared/disappeared, rates might have moved.
        delegate.observeGraph()
            .onEach { cache.clear() }
            .map { it } // identity hop, kept so a future decorator can extend the pipeline

    /** Cached direct-rate lookup. Falls through to the graph on miss. */
    fun directRate(graph: CurrencyGraph, from: Currency, to: Currency): Double? {
        cache[from to to]?.let { return it }
        val rate = graph.directRate(from, to) ?: return null
        cache[from to to] = rate
        return rate
    }

    private companion object {
        private const val DEFAULT_CAPACITY = 256
    }
}
