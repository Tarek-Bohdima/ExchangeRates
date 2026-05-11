package com.tarek.exchangerates.domain.model

import java.math.BigDecimal
import java.math.MathContext
import java.math.RoundingMode

/**
 * A currency-tagged amount.
 *
 * Money is kept in [BigDecimal] so we never lose precision through repeated
 * floating-point multiplications when chaining conversions; the rate itself can
 * stay as [Double] for graph algorithms, and only the final amount is folded
 * back into [BigDecimal].
 */
data class Money(
    val amount: BigDecimal,
    val currency: Currency,
) : Comparable<Money> {

    init {
        require(amount.signum() >= 0) { "Money amount must be non-negative, got $amount" }
    }

    operator fun times(factor: Double): Money =
        copy(amount = amount.multiply(BigDecimal(factor), MATH_CONTEXT))

    fun convertedTo(target: Currency, rate: Double): Money =
        Money(amount = amount.multiply(BigDecimal(rate), MATH_CONTEXT), currency = target)

    fun rounded(scale: Int = DISPLAY_SCALE): Money =
        copy(amount = amount.setScale(scale, RoundingMode.HALF_EVEN))

    override fun compareTo(other: Money): Int {
        require(currency == other.currency) {
            "Cannot compare Money values of different currencies ($currency vs ${other.currency})"
        }
        return amount.compareTo(other.amount)
    }

    override fun toString(): String = "${rounded().amount.toPlainString()} $currency"

    companion object {
        private const val DISPLAY_SCALE = 4
        private val MATH_CONTEXT = MathContext.DECIMAL64

        fun of(amount: String, currency: Currency): Money =
            Money(BigDecimal(amount), currency)

        fun of(amount: Double, currency: Currency): Money =
            Money(BigDecimal(amount, MATH_CONTEXT), currency)
    }
}
