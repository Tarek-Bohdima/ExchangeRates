package com.terraconnect.exchangerates.data.remote.dto

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import com.terraconnect.exchangerates.models.ExchangeRates
import com.terraconnect.exchangerates.models.Pairs
import com.terraconnect.exchangerates.models.Rate

@JsonClass(generateAdapter = true)
data class ExchangeRatesDTO(

    @Json(name = "rates")
    val rates: List<RatesDTO>,

    @Json(name = "pairs")
    val pairs: List<PairsDTO>,
)

/**
 * Convert Network results to domain objects
 */
fun ExchangeRatesDTO.asRatesDomainModel(): List<Rate> {
    return rates.map {
        Rate(
            from = it.from,
            to = it.to,
            rate = it.rate
        )
    }
}

fun ExchangeRatesDTO.asPairsDomainModel(): List<Pairs> {
    return pairs.map {
        Pairs(
            from = it.from,
            to = it.to
        )
    }
}
fun ExchangeRatesDTO.asDomainModel(): ExchangeRates {
    val rates = this.asRatesDomainModel()
    val pairs = this.asPairsDomainModel()
    return  ExchangeRates(rates, pairs)
}