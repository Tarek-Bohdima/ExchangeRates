package com.tarek.exchangerates.domain.model

/**
 * A cycle of currencies whose product of rates exceeds 1.0 — i.e. starting
 * with X units of [cycle].first() and following the cycle yields strictly more
 * than X units of the same currency.
 *
 * The detection algorithm transforms each edge's rate into `-ln(rate)` and runs
 * Bellman-Ford; a negative-weight cycle in that transformed graph is an
 * arbitrage opportunity in the original one.
 */
data class ArbitrageOpportunity(
    val cycle: List<Currency>,
    val profitFactor: Double,
) {
    init {
        require(cycle.size >= 2) { "Arbitrage cycle must have at least 2 nodes, got ${cycle.size}" }
        require(cycle.first() == cycle.last()) {
            "Arbitrage cycle must start and end at the same currency"
        }
        require(profitFactor > 1.0) {
            "ProfitFactor must be > 1.0 to be an arbitrage, got $profitFactor"
        }
    }

    val profitPercent: Double get() = (profitFactor - 1.0) * 100.0
}
