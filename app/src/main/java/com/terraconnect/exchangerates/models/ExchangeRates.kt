package com.terraconnect.exchangerates.models

data class ExchangeRates(
    val rates: List<Rates>,
    val pairs: List<Pairs>,
)