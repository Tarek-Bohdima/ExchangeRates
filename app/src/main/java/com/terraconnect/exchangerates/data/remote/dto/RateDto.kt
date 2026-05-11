package com.terraconnect.exchangerates.data.remote.dto

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

/** Wire representation of a directed rate edge. */
@JsonClass(generateAdapter = true)
data class RateDto(
    @Json(name = "from") val from: String,
    @Json(name = "to") val to: String,
    @Json(name = "rate") val rate: Double,
)
