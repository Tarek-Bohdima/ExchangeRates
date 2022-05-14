package com.terraconnect.exchangerates.data.remote

import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import com.terraconnect.exchangerates.data.Constants
import com.terraconnect.exchangerates.data.remote.dto.ExchangeRatesDTO
import retrofit2.Call
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.GET

private val moshi = Moshi.Builder()
    .add(KotlinJsonAdapterFactory())
    .build()

private val retrofit = Retrofit.Builder()
    .addConverterFactory(MoshiConverterFactory.create(moshi))
    .baseUrl(Constants.BASE_URL)
    .build()

interface RatesApiService {
    @GET("rates2.json")
    suspend fun getRates(): ExchangeRatesDTO
}

object RatesApi {
    val retrofitService: RatesApiService by lazy { retrofit.create(RatesApiService::class.java) }
}