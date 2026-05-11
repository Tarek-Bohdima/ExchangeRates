package com.terraconnect.exchangerates.domain.model

/**
 * ISO-4217-style currency code wrapped as an inline value class.
 *
 * Using a value class keeps the runtime representation as a plain [String] (no
 * allocation overhead) while giving the type system enough information to stop
 * mistakes like passing a country code where a currency code is expected.
 *
 * Construction must go through [of] so the invariant — three uppercase ASCII
 * letters — is enforced exactly once at the boundary.
 */
@JvmInline
value class Currency private constructor(val code: String) : Comparable<Currency> {

    override fun toString(): String = code

    override fun compareTo(other: Currency): Int = code.compareTo(other.code)

    companion object {
        private val CODE_REGEX = Regex("^[A-Z]{3}$")

        fun of(code: String): Currency {
            val normalized = code.trim().uppercase()
            require(CODE_REGEX.matches(normalized)) {
                "Currency code must be 3 uppercase ASCII letters, got '$code'"
            }
            return Currency(normalized)
        }

        fun ofOrNull(code: String?): Currency? =
            code?.let { runCatching { of(it) }.getOrNull() }
    }
}
