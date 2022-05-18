package com.terraconnect.exchangerates.data.remote.dto

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import com.terraconnect.exchangerates.models.Rates

@JsonClass(generateAdapter = true)
data class RatesDTO(

    @Json(name = "from")
    val from: String,

    @Json(name = "to")
    val to: String,

    @Json(name = "rate")
    val rate: Double,
)