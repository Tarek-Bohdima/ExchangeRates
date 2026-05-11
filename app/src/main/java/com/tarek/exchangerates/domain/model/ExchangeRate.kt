package com.tarek.exchangerates.domain.model

/**
 * A single directed edge in the conversion graph: 1 unit of [from] = [rate] units of [to].
 *
 * The reciprocal edge is intentionally *not* implied — some real markets quote
 * asymmetric bid/ask spreads, and the graph treats every edge independently.
 */
data class ExchangeRate(
    val from: Currency,
    val to: Currency,
    val rate: Double,
) {
    init {
        require(rate > 0.0 && rate.isFinite()) {
            "Exchange rate must be a positive finite number, got $rate ($from -> $to)"
        }
        require(from != to) { "Self-edge not allowed ($from -> $to)" }
    }

    /** Returns the inverse edge — useful when a real backend only quotes one direction. */
    fun inverse(): ExchangeRate = ExchangeRate(from = to, to = from, rate = 1.0 / rate)
}
