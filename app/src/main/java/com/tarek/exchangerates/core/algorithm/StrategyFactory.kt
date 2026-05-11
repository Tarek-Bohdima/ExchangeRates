package com.tarek.exchangerates.core.algorithm

import javax.inject.Inject
import javax.inject.Singleton

/**
 * Factory (GoF) for path-finding strategies.
 *
 * Exposed as an interface so callers (use cases, tests) depend on the
 * abstraction, not the implementation. The DI graph picks the concrete
 * implementation; everything else only sees the contract: "I can give you a
 * strategy if you tell me which kind."
 *
 * That separation pays off in tests — `ConvertCurrencyUseCase` can be
 * exercised with a hand-rolled fake factory that returns a known strategy,
 * without spinning up Hilt or the real multibinding map.
 */
interface StrategyFactory {
    val available: Set<StrategyKind>
    fun get(kind: StrategyKind): PathFindingStrategy
}

/**
 * Default implementation. Hilt builds the [strategies] map via @IntoMap
 * multibinding (see `AlgorithmModule`), so adding a new algorithm is a
 * single @Binds line — no edits here.
 */
@Singleton
class DefaultStrategyFactory @Inject constructor(
    private val strategies: Map<StrategyKind, @JvmSuppressWildcards PathFindingStrategy>,
) : StrategyFactory {

    override val available: Set<StrategyKind> get() = strategies.keys

    override fun get(kind: StrategyKind): PathFindingStrategy =
        strategies[kind] ?: error("No PathFindingStrategy bound for $kind")
}
