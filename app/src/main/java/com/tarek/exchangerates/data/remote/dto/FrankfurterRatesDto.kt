package com.tarek.exchangerates.data.remote.dto

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import com.tarek.exchangerates.domain.model.Currency
import com.tarek.exchangerates.domain.model.ExchangeRate

/**
 * Wire-format envelope for the Frankfurter API (`/v1/latest?base=XXX`).
 *
 * Frankfurter quotes one base currency against ~30 others, ECB-backed. The
 * payload is a flat map of `currencyCode -> rate`, which is more idiomatic to
 * decode as `Map<String, Double>` than as a list of pair-objects.
 *
 * This DTO is the second worked example of the Adapter pattern (GoF) in the
 * codebase: a wire format that doesn't match the domain model gets translated
 * at *exactly one* place — the [toDomain] extension below — and the rest of
 * the app never knows the difference.
 */
@JsonClass(generateAdapter = true)
data class FrankfurterRatesDto(
    @Json(name = "base") val base: String,
    @Json(name = "date") val date: String,
    @Json(name = "rates") val rates: Map<String, Double>,
)

/**
 * Map a Frankfurter response into directed graph edges.
 *
 * Frankfurter only quotes *one direction* per call (base → quote), so the
 * raw response would produce a star graph — every conversion is 1 hop from
 * the base. Setting [addReciprocals] = true synthesises the inverse edges as
 * well, turning the star into a hub-and-spoke graph where any two non-base
 * currencies can convert via the base in 2 hops. Strictly arbitrage-free
 * (since the inverses are algebraically derived) but at least the graph
 * algorithms have *something* to traverse.
 *
 * Malformed entries — non-ISO codes, zero or negative rates, or NaN — are
 * silently dropped rather than failing the whole batch. Same pragmatism as
 * the embedded dataset.
 */
fun FrankfurterRatesDto.toDomain(addReciprocals: Boolean = true): List<ExchangeRate> {
    val baseCurrency = Currency.ofOrNull(base) ?: return emptyList()
    val forwardEdges = rates.mapNotNull { (code, rate) ->
        val target = Currency.ofOrNull(code) ?: return@mapNotNull null
        if (target == baseCurrency) return@mapNotNull null
        runCatching { ExchangeRate(baseCurrency, target, rate) }.getOrNull()
    }
    return if (addReciprocals) {
        forwardEdges.flatMap { listOf(it, it.inverse()) }
    } else {
        forwardEdges
    }
}
