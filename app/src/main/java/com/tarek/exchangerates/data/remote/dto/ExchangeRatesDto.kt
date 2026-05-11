package com.tarek.exchangerates.data.remote.dto

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import com.tarek.exchangerates.domain.model.Currency
import com.tarek.exchangerates.domain.model.ExchangeRate

/**
 * Wire-format envelope for the whole rates payload.
 *
 * DTOs deliberately stay in this package and never leak past the data layer.
 * The [toDomain] adapter below is the only bridge between "shape the network
 * gave us" and "types the domain wants to think in" — that single boundary is
 * the GoF Adapter pattern.
 *
 * Keeping Moshi annotations off the domain model means we can swap Moshi for
 * kotlinx-serialization tomorrow without touching anything above this layer.
 */
@JsonClass(generateAdapter = true)
data class ExchangeRatesDto(
    @Json(name = "rates") val rates: List<RateDto>,
    @Json(name = "pairs") val pairs: List<PairDto>,
)

/**
 * Convert a list of [RateDto]s into well-typed domain edges, silently dropping
 * any malformed entry (bad currency code, non-positive rate, self-edge). It's
 * a pragmatic choice: we'd rather render 99% of a useful graph than crash on
 * one bad row from a fresh backend.
 */
fun List<RateDto>.toDomain(): List<ExchangeRate> = mapNotNull { dto ->
    val from = Currency.ofOrNull(dto.from) ?: return@mapNotNull null
    val to = Currency.ofOrNull(dto.to) ?: return@mapNotNull null
    if (from == to) return@mapNotNull null
    runCatching { ExchangeRate(from, to, dto.rate) }.getOrNull()
}
