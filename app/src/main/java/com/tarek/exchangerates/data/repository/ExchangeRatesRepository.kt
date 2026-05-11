package com.tarek.exchangerates.data.repository

import com.tarek.exchangerates.core.ds.CurrencyGraph
import kotlinx.coroutines.flow.Flow

/**
 * Single source of truth for the rate graph from the rest of the app's
 * perspective. Returns a [Flow] so the UI can react when rates are refreshed.
 */
interface ExchangeRatesRepository {
    fun observeGraph(): Flow<CurrencyGraph>
    suspend fun refresh()
}
