package com.tarek.exchangerates.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class CurrencyTest {

    @Test
    fun `of accepts valid 3-letter uppercase ISO code`() {
        val usd = Currency.of("USD")
        assertEquals("USD", usd.code)
    }

    @Test
    fun `of trims and uppercases input`() {
        val eur = Currency.of("  eur  ")
        assertEquals("EUR", eur.code)
    }

    @Test
    fun `of rejects too-short codes`() {
        assertThrows(IllegalArgumentException::class.java) { Currency.of("US") }
    }

    @Test
    fun `of rejects too-long codes`() {
        assertThrows(IllegalArgumentException::class.java) { Currency.of("USDD") }
    }

    @Test
    fun `of rejects codes with digits`() {
        assertThrows(IllegalArgumentException::class.java) { Currency.of("U5D") }
    }

    @Test
    fun `of rejects codes with non-ASCII letters`() {
        assertThrows(IllegalArgumentException::class.java) { Currency.of("EÜR") }
    }

    @Test
    fun `ofOrNull returns null for invalid input instead of throwing`() {
        assertNull(Currency.ofOrNull(""))
        assertNull(Currency.ofOrNull("US"))
        assertNull(Currency.ofOrNull(null))
    }

    @Test
    fun `ofOrNull returns Currency for valid input`() {
        val gbp = Currency.ofOrNull(" gbp ")
        assertNotNull(gbp)
        assertEquals("GBP", gbp!!.code)
    }

    @Test
    fun `currencies are ordered alphabetically`() {
        val sorted = listOf("USD", "EUR", "AUD").map(Currency::of).sorted()
        assertEquals(listOf("AUD", "EUR", "USD"), sorted.map { it.code })
    }

    @Test
    fun `equality is structural`() {
        assertEquals(Currency.of("USD"), Currency.of("USD"))
        assertTrue(Currency.of("USD") != Currency.of("EUR"))
    }
}
