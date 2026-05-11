package com.tarek.exchangerates.di

import com.tarek.exchangerates.core.algorithm.BellmanFordPathFindingStrategy
import com.tarek.exchangerates.core.algorithm.BfsPathFindingStrategy
import com.tarek.exchangerates.core.algorithm.DefaultStrategyFactory
import com.tarek.exchangerates.core.algorithm.DijkstraPathFindingStrategy
import com.tarek.exchangerates.core.algorithm.DirectLookupStrategy
import com.tarek.exchangerates.core.algorithm.FloydWarshallPathFindingStrategy
import com.tarek.exchangerates.core.algorithm.PathFindingStrategy
import com.tarek.exchangerates.core.algorithm.StrategyFactory
import com.tarek.exchangerates.core.algorithm.StrategyKind
import dagger.Binds
import dagger.MapKey
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoMap

/**
 * Hilt module that wires every [PathFindingStrategy] into a single map keyed
 * by [StrategyKind] — the bedrock of the Strategy + Factory combo.
 *
 * Why multi-binding? `StrategyFactory` shouldn't have to know "here are the
 * five concrete classes I might be asked for". Adding a new algorithm should
 * be a one-line change in this module, nothing else. That's what `@IntoMap`
 * + `@StrategyKey` deliver: the DI graph maintains the catalogue.
 *
 * The `@MapKey` annotation below declares that we'll use [StrategyKind] enum
 * values as keys; `unwrapValue = true` means the *enum value itself* is the
 * key, not a wrapper object.
 */
@MapKey
annotation class StrategyKey(val value: StrategyKind)

@Module
@InstallIn(SingletonComponent::class)
abstract class AlgorithmModule {

    @Binds
    @IntoMap
    @StrategyKey(StrategyKind.DIRECT)
    abstract fun bindDirect(impl: DirectLookupStrategy): PathFindingStrategy

    @Binds
    @IntoMap
    @StrategyKey(StrategyKind.BFS)
    abstract fun bindBfs(impl: BfsPathFindingStrategy): PathFindingStrategy

    @Binds
    @IntoMap
    @StrategyKey(StrategyKind.DIJKSTRA)
    abstract fun bindDijkstra(impl: DijkstraPathFindingStrategy): PathFindingStrategy

    @Binds
    @IntoMap
    @StrategyKey(StrategyKind.BELLMAN_FORD)
    abstract fun bindBellmanFord(impl: BellmanFordPathFindingStrategy): PathFindingStrategy

    @Binds
    @IntoMap
    @StrategyKey(StrategyKind.FLOYD_WARSHALL)
    abstract fun bindFloydWarshall(impl: FloydWarshallPathFindingStrategy): PathFindingStrategy

    @Binds
    abstract fun bindStrategyFactory(impl: DefaultStrategyFactory): StrategyFactory
}
