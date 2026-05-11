package com.tarek.exchangerates.data.source

import com.tarek.exchangerates.data.remote.RatesApiService
import com.tarek.exchangerates.data.remote.dto.toDomain
import com.tarek.exchangerates.domain.model.ExchangeRate
import javax.inject.Inject

/**
 * Frankfurter-backed data source.
 *
 * Stays behind the [ExchangeRatesDataSource] interface, so switching from the
 * embedded dataset to a live backend is a one-line change in `DataModule`.
 *
 * Frankfurter only quotes one base per call (USD by default here). We ask for
 * reciprocals so the resulting graph is a hub-and-spoke around USD — any two
 * non-base currencies can convert via USD in 2 hops. That's strictly
 * arbitrage-free (the inverses are algebraically derived) but it does at
 * least give every strategy a small graph to traverse.
 */
class RemoteExchangeRatesDataSource @Inject constructor(
    private val api: RatesApiService,
) : ExchangeRatesDataSource {

    override suspend fun fetchRates(): List<ExchangeRate> {
        val response = api.getLatestRates(base = DEFAULT_BASE)
        val body = response.body()
        if (!response.isSuccessful || body == null) {
            error("Rates request failed: HTTP ${response.code()} ${response.message()}")
        }
        return body.toDomain(addReciprocals = true)
    }

    private companion object {
        private const val DEFAULT_BASE = "USD"
    }
}
