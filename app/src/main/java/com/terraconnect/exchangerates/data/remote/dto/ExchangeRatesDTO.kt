package com.terraconnect.exchangerates.data.remote.dto

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class ExchangeRatesDTO(

    @Json(name = "rates")
    val rates: List<RatesDTO>,

    @Json(name = "pairs")
    val pairs: List<PairsDTO>,
)