package com.tarek.exchangerates.data.source

import com.tarek.exchangerates.domain.model.ExchangeRate

/**
 * Abstraction over "wherever the rates come from".
 *
 * Two impls in the codebase:
 *  * [EmbeddedExchangeRatesDataSource] — hand-curated graph for local demos.
 *  * [RemoteExchangeRatesDataSource] — Retrofit-backed for a real backend.
 *
 * The repository picks one (or stacks them) via DI; everything above the data
 * layer only ever sees this interface, never the concrete class. That's what
 * lets the same ViewModel boot the app offline (embedded) or online (remote)
 * with zero code change.
 */
interface ExchangeRatesDataSource {
    suspend fun fetchRates(): List<ExchangeRate>
}
