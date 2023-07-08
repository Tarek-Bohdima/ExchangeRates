package com.terraconnect.exchangerates.util

import com.terraconnect.exchangerates.models.Rate

class RateCalculator(rates: List<Rate>) {

    private val rateMap = mutableMapOf<String, MutableMap<String, Double>>()

    init {
        rates.forEach { rate ->
            if (!rateMap.containsKey(rate.from)) {
                rateMap[rate.from] = mutableMapOf()
            }
            rateMap[rate.from]?.put(rate.to, rate.rate)
        }
    }

    fun calculateRate(from: String, to: String): Double? {
        return calculateRate(from, to, mutableSetOf())
    }

    private fun calculateRate(from: String, to: String, visited: MutableSet<String>): Double? {
        if (rateMap[from]?.containsKey(to) == true) {
            return rateMap[from]?.get(to)
        }

        val rates = rateMap[from]
        visited.add(from)

        rates?.keys?.forEach { intermediate ->
            if (!visited.contains(intermediate)) {
                val rate = calculateRate(intermediate, to, visited)
                if (rate != null) {
                    return rates[intermediate]!! * rate
                }
            }
        }
        return null
    }
}
