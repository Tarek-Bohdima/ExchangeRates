package com.terraconnect.exchangerates.data.remote.dto

data class ExchangeRatesDTO(
    val rates: List<RatesDTO>,
    val pairs: List<PairsDTO>,
)