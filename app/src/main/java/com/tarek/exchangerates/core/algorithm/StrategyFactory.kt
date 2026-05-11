package com.tarek.exchangerates.core.algorithm

import javax.inject.Inject
import javax.inject.Singleton

/**
 * Factory (GoF) for path-finding strategies.
 *
 * Hilt builds the [strategies] map via @IntoMap multi-binding, so adding a new
 * algorithm is a single `@Binds @IntoMap @StrategyKey(...)` line in
 * `AlgorithmModule` — no edits to this factory needed. That's the value of
 * keeping the factory dumb and pushing knowledge of "what concrete classes
 * exist" out to the DI layer.
 */
@Singleton
class StrategyFactory @Inject constructor(
    private val strategies: Map<StrategyKind, @JvmSuppressWildcards PathFindingStrategy>,
) {

    val available: Set<StrategyKind> get() = strategies.keys

    fun get(kind: StrategyKind): PathFindingStrategy =
        strategies[kind] ?: error("No PathFindingStrategy bound for $kind")
}
