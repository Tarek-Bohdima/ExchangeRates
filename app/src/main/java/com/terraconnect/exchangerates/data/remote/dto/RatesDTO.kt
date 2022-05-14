package com.terraconnect.exchangerates.data.remote.dto

data class RatesDTO(
    val from: String,
    val to: String,
    val rate: Double,
)