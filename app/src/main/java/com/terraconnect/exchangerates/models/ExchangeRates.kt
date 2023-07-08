package com.terraconnect.exchangerates.models

data class ExchangeRates(
    val rates: List<Rate>,
    val pairs: List<Pairs>,
)