package com.tarek.exchangerates.domain.model

/**
 * Outcome of a conversion attempt.
 *
 * Modelled as a sealed interface so callers must handle every branch — no
 * silent "else -> {}" the way the original ViewModel did with its old `Result`.
 */
sealed interface ConversionResult {

    data class Success(
        val path: ConversionPath,
        val output: Money,
    ) : ConversionResult {
        val rate: Double get() = path.compositeRate
    }

    /** Source and target are not connected in the rate graph. */
    data class Unreachable(val from: Currency, val to: Currency) : ConversionResult

    /** Source or target currency does not exist in the graph at all. */
    data class UnknownCurrency(val currency: Currency) : ConversionResult

    /** Algorithm-level error or unexpected failure (e.g. arbitrage detected when not allowed). */
    data class Failure(val reason: String, val cause: Throwable? = null) : ConversionResult
}
