package com.terraconnect.exchangerates.data.source

import com.terraconnect.exchangerates.data.remote.RatesApiService
import com.terraconnect.exchangerates.data.remote.dto.toDomain
import com.terraconnect.exchangerates.domain.model.ExchangeRate
import javax.inject.Inject

/**
 * Retrofit-backed data source. Stays *behind the [ExchangeRatesDataSource]
 * interface*, so swapping it in is one DI binding change — see the
 * [com.terraconnect.exchangerates.di.DataModule].
 */
class RemoteExchangeRatesDataSource @Inject constructor(
    private val api: RatesApiService,
) : ExchangeRatesDataSource {

    override suspend fun fetchRates(): List<ExchangeRate> {
        val response = api.getRates()
        val body = response.body()
        if (!response.isSuccessful || body == null) {
            error("Rates request failed: HTTP ${response.code()} ${response.message()}")
        }
        return body.rates.toDomain()
    }
}
