package com.tarek.exchangerates.data.remote.dto

import com.tarek.exchangerates.domain.model.Currency
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class FrankfurterRatesDtoTest {

    private val usd = Currency.of("USD")
    private val eur = Currency.of("EUR")
    private val gbp = Currency.of("GBP")

    @Test
    fun `toDomain produces one forward edge per quote when reciprocals disabled`() {
        val dto = FrankfurterRatesDto(
            base = "USD",
            date = "2026-05-11",
            rates = mapOf("EUR" to 0.92, "GBP" to 0.78),
        )
        val edges = dto.toDomain(addReciprocals = false)
        assertEquals(2, edges.size)
        assertTrue(edges.all { it.from == usd })
        assertEquals(setOf(eur, gbp), edges.map { it.to }.toSet())
    }

    @Test
    fun `toDomain with reciprocals doubles edges and inverts rates`() {
        val dto = FrankfurterRatesDto(
            base = "USD",
            date = "2026-05-11",
            rates = mapOf("EUR" to 0.92),
        )
        val edges = dto.toDomain(addReciprocals = true)
        assertEquals(2, edges.size)
        val forward = edges.first { it.from == usd }
        val reverse = edges.first { it.from == eur }
        assertEquals(0.92, forward.rate, 1e-12)
        assertEquals(1.0 / 0.92, reverse.rate, 1e-12)
    }

    @Test
    fun `toDomain drops self-quotes`() {
        // Frankfurter sometimes echoes the base in the rates map with rate 1.0.
        val dto = FrankfurterRatesDto(
            base = "USD",
            date = "2026-05-11",
            rates = mapOf("USD" to 1.0, "EUR" to 0.92),
        )
        val edges = dto.toDomain(addReciprocals = false)
        assertEquals(1, edges.size) // USD -> USD filtered out
        assertEquals(eur, edges.single().to)
    }

    @Test
    fun `toDomain drops malformed currency codes`() {
        val dto = FrankfurterRatesDto(
            base = "USD",
            date = "2026-05-11",
            rates = mapOf("EUR" to 0.92, "BAD CODE" to 1.0, "123" to 1.0),
        )
        val edges = dto.toDomain(addReciprocals = false)
        assertEquals(1, edges.size)
        assertEquals(eur, edges.single().to)
    }

    @Test
    fun `toDomain drops zero or negative rates`() {
        val dto = FrankfurterRatesDto(
            base = "USD",
            date = "2026-05-11",
            rates = mapOf("EUR" to 0.0, "GBP" to -1.0, "JPY" to 154.0),
        )
        val edges = dto.toDomain(addReciprocals = false)
        assertEquals(1, edges.size) // only JPY survives
        assertEquals(Currency.of("JPY"), edges.single().to)
    }

    @Test
    fun `toDomain returns empty list when base fails format validation`() {
        // 4-letter code — Currency.of's regex requires exactly 3 uppercase ASCII letters.
        val dto = FrankfurterRatesDto(
            base = "USDX",
            date = "2026-05-11",
            rates = mapOf("EUR" to 0.92),
        )
        assertTrue(dto.toDomain().isEmpty())
    }
}
