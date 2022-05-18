package com.terraconnect.exchangerates.data.remote

import com.terraconnect.exchangerates.data.remote.dto.ExchangeRatesDTO
import retrofit2.Response
import retrofit2.http.GET


interface RatesApiService {
    @GET("rates2.json")
    suspend fun getRates(): Response<ExchangeRatesDTO>
}
