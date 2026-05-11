package com.tarek.exchangerates.data.remote

import com.tarek.exchangerates.data.remote.dto.FrankfurterRatesDto
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Query

/**
 * Retrofit interface to the Frankfurter API (https://www.frankfurter.dev/).
 *
 * Kept deliberately thin — the [com.tarek.exchangerates.data.source.RemoteExchangeRatesDataSource]
 * does the DTO → domain mapping. This file only knows about transport.
 *
 * The default base parameter is USD because that's what the rest of the demo
 * defaults to; the caller can override.
 */
interface RatesApiService {
    @GET("v1/latest")
    suspend fun getLatestRates(
        @Query("base") base: String = "USD",
    ): Response<FrankfurterRatesDto>
}
