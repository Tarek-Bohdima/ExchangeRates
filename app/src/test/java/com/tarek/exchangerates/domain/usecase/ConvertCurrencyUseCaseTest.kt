package com.tarek.exchangerates.domain.usecase

import com.tarek.exchangerates.core.algorithm.PathFindingStrategy
import com.tarek.exchangerates.core.algorithm.StrategyFactory
import com.tarek.exchangerates.core.algorithm.StrategyKind
import com.tarek.exchangerates.core.ds.CurrencyGraph
import com.tarek.exchangerates.domain.model.ConversionResult
import com.tarek.exchangerates.domain.model.Currency
import com.tarek.exchangerates.domain.model.ExchangeRate
import com.tarek.exchangerates.domain.model.Money
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ConvertCurrencyUseCaseTest {

    private val usd = Currency.of("USD")
    private val eur = Currency.of("EUR")
    private val gbp = Currency.of("GBP")

    private val graph = CurrencyGraph.builder()
        .addRate(ExchangeRate(usd, eur, 0.90))
        .addRate(ExchangeRate(eur, gbp, 0.85))
        .build()

    /**
     * Tiny hand-rolled fake — proves the use case depends only on the
     * StrategyFactory *abstraction*, not on Hilt or any concrete impl.
     */
    private fun factoryReturning(strategy: PathFindingStrategy): StrategyFactory =
        object : StrategyFactory {
            override val available = setOf(StrategyKind.DIJKSTRA)
            override fun get(kind: StrategyKind): PathFindingStrategy = strategy
        }

    @Test
    fun `returns Success with output amount equal to input times composite rate`() {
        val useCase = ConvertCurrencyUseCase(
            factoryReturning(
                PathFindingStrategy { _, from, _ ->
                    // Single-edge path with composite rate 0.90.
                    com.tarek.exchangerates.domain.model.ConversionPath(
                        source = from,
                        edges = listOf(ExchangeRate(usd, eur, 0.90)),
                    )
                },
            ),
        )
        val result = useCase(
            graph = graph,
            input = Money.of("100", usd),
            target = eur,
            kind = StrategyKind.DIJKSTRA,
        )
        assertTrue(result is ConversionResult.Success)
        val success = result as ConversionResult.Success
        assertEquals(eur, success.output.currency)
        assertEquals(0, success.output.amount.compareTo(java.math.BigDecimal("90.0")))
    }

    @Test
    fun `returns UnknownCurrency when source is not in graph`() {
        val xyz = Currency.of("XYZ")
        val useCase = ConvertCurrencyUseCase(
            factoryReturning(PathFindingStrategy { _, _, _ -> null }),
        )
        val result = useCase(graph = graph, input = Money.of("10", xyz), target = eur, kind = StrategyKind.DIJKSTRA)
        assertTrue(result is ConversionResult.UnknownCurrency)
        assertEquals(xyz, (result as ConversionResult.UnknownCurrency).currency)
    }

    @Test
    fun `returns UnknownCurrency when target is not in graph`() {
        val xyz = Currency.of("XYZ")
        val useCase = ConvertCurrencyUseCase(
            factoryReturning(PathFindingStrategy { _, _, _ -> null }),
        )
        val result = useCase(graph = graph, input = Money.of("10", usd), target = xyz, kind = StrategyKind.DIJKSTRA)
        assertTrue(result is ConversionResult.UnknownCurrency)
    }

    @Test
    fun `returns Unreachable when strategy returns null on a connected graph`() {
        val useCase = ConvertCurrencyUseCase(
            factoryReturning(PathFindingStrategy { _, _, _ -> null }),
        )
        val result = useCase(graph = graph, input = Money.of("10", usd), target = gbp, kind = StrategyKind.DIJKSTRA)
        assertTrue(result is ConversionResult.Unreachable)
    }

    @Test
    fun `same-source-and-target produces an identity Success`() {
        val useCase = ConvertCurrencyUseCase(
            factoryReturning(PathFindingStrategy { _, _, _ -> error("Should not be called for identity") }),
        )
        val result = useCase(graph = graph, input = Money.of("42", usd), target = usd, kind = StrategyKind.DIJKSTRA)
        assertTrue(result is ConversionResult.Success)
        val success = result as ConversionResult.Success
        assertEquals(usd, success.output.currency)
        assertTrue(success.path.isIdentity)
        assertEquals(0, success.output.amount.compareTo(java.math.BigDecimal("42")))
    }
}
