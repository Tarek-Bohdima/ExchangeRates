package com.tarek.exchangerates.data.source

import com.tarek.exchangerates.domain.model.Currency
import com.tarek.exchangerates.domain.model.ExchangeRate
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Hand-curated rate graph that ships with the app.
 *
 * Two design notes:
 *  * Numbers are *roughly* realistic but not pegged to today's market — this
 *    is a study app, not a trading desk.
 *  * The dataset deliberately contains a tiny **arbitrage triangle** so the
 *    Bellman-Ford branch has something to find. Read the table below: the
 *    `USD → EUR → GBP → USD` loop multiplies to a number slightly greater
 *    than 1.0, which is exactly what the arbitrage detector lights up on.
 *
 * Numbers are independent (not algebraically derived) so the graph is *not*
 * accidentally arbitrage-free — that "imperfection" is the whole point.
 */
@Singleton
class EmbeddedExchangeRatesDataSource @Inject constructor() : ExchangeRatesDataSource {

    override suspend fun fetchRates(): List<ExchangeRate> = RATES

    private companion object {
        // Currency code shorthand — keeps the table below readable.
        private val USD = Currency.of("USD")
        private val EUR = Currency.of("EUR")
        private val GBP = Currency.of("GBP")
        private val JPY = Currency.of("JPY")
        private val CHF = Currency.of("CHF")
        private val CAD = Currency.of("CAD")
        private val AUD = Currency.of("AUD")
        private val CNY = Currency.of("CNY")
        private val INR = Currency.of("INR")
        private val BRL = Currency.of("BRL")
        private val SEK = Currency.of("SEK")
        private val NOK = Currency.of("NOK")

        private val RATES: List<ExchangeRate> = listOf(
            // ===== USD-centric edges (major pairs) =====
            ExchangeRate(USD, EUR, 0.9250),
            ExchangeRate(USD, GBP, 0.7910),
            ExchangeRate(USD, JPY, 154.20),
            ExchangeRate(USD, CHF, 0.8830),
            ExchangeRate(USD, CAD, 1.3650),
            ExchangeRate(USD, AUD, 1.5120),
            ExchangeRate(USD, CNY, 7.2300),
            ExchangeRate(USD, INR, 83.450),
            ExchangeRate(USD, BRL, 5.0800),
            ExchangeRate(USD, SEK, 10.420),
            ExchangeRate(USD, NOK, 10.730),

            // ===== The arbitrage triangle =====
            // USD -> EUR -> GBP -> USD should ideally multiply to ~1.0; here
            // we set the edges to compound to ~1.012, a 1.2% arbitrage.
            // Calculation: 0.9250 * 0.8580 * 1.2750 = 1.01186 (≈ +1.19%).
            ExchangeRate(EUR, GBP, 0.8580),
            ExchangeRate(GBP, USD, 1.2750),

            // ===== Cross rates (no arbitrage, just realistic crosses) =====
            ExchangeRate(EUR, JPY, 166.70),
            ExchangeRate(EUR, CHF, 0.9540),
            ExchangeRate(GBP, EUR, 1.1640),
            ExchangeRate(GBP, JPY, 194.20),
            ExchangeRate(CHF, USD, 1.1325),
            ExchangeRate(CAD, USD, 0.7325),
            ExchangeRate(AUD, USD, 0.6610),
            ExchangeRate(JPY, USD, 0.00648),
            ExchangeRate(CNY, USD, 0.1383),
            ExchangeRate(INR, USD, 0.01198),

            // ===== A few "thin" markets that force multi-hop conversion =====
            // BRL only quotes against USD on this graph, forcing any BRL→X
            // path to first go through USD. SEK & NOK only round-trip via USD.
            ExchangeRate(BRL, USD, 0.1968),
            ExchangeRate(SEK, USD, 0.0959),
            ExchangeRate(NOK, USD, 0.0931),

            // ===== Asia/Australia secondary edges =====
            ExchangeRate(AUD, JPY, 101.94),
            ExchangeRate(CNY, JPY, 21.330),
        )
    }
}
