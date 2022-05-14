package com.terraconnect.exchangerates.models

data class Rates(
    val from: String,
    val to: String,
    val rate: Double,
)