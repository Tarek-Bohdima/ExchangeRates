package com.terraconnect.exchangerates.data.remote

import com.terraconnect.exchangerates.data.remote.dto.ExchangeRatesDto
import retrofit2.Response
import retrofit2.http.GET

/**
 * Retrofit interface to a remote exchange-rate backend.
 *
 * Kept deliberately thin — the [RemoteExchangeRatesDataSource] is what does
 * the DTO→domain mapping. This file only knows about transport.
 */
interface RatesApiService {
    @GET("rates")
    suspend fun getRates(): Response<ExchangeRatesDto>
}
