package com.terraconnect.exchangerates.models

data class Rate(
    val from: String,
    val to: String,
    val rate: Double,
)