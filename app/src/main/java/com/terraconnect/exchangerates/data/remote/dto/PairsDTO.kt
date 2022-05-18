package com.terraconnect.exchangerates.data.remote.dto

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import com.terraconnect.exchangerates.models.Pairs

@JsonClass(generateAdapter = true)
data class PairsDTO(

    @Json(name = "from")
    val from: String,

    @Json(name = "to")
    val to: String,
)