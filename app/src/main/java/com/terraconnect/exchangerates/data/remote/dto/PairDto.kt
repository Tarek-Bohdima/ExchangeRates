package com.terraconnect.exchangerates.data.remote.dto

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

/** Wire representation of a pair the backend wants us to compute for. */
@JsonClass(generateAdapter = true)
data class PairDto(
    @Json(name = "from") val from: String,
    @Json(name = "to") val to: String,
)
