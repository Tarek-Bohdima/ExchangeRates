package com.tarek.exchangerates.di

import com.tarek.exchangerates.data.repository.CachingExchangeRatesRepository
import com.tarek.exchangerates.data.repository.DefaultExchangeRatesRepository
import com.tarek.exchangerates.data.repository.ExchangeRatesRepository
import com.tarek.exchangerates.data.source.EmbeddedExchangeRatesDataSource
import com.tarek.exchangerates.data.source.ExchangeRatesDataSource
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Wires the data layer.
 *
 * Two design points worth pointing out:
 *
 * 1. **Source swap.** `ExchangeRatesDataSource` is bound to the **embedded**
 *    implementation. To run against the Frankfurter API
 *    (https://www.frankfurter.dev/), change the binding target on the
 *    `@Binds` below from `EmbeddedExchangeRatesDataSource` to
 *    `RemoteExchangeRatesDataSource` — *that's the entire change*. The
 *    repository, use cases, ViewModel, and UI are completely insulated from
 *    where the rates come from. That's the payoff of coding to interfaces.
 *
 *    Why embedded is the default: the embedded dataset is hand-curated to
 *    contain a deliberate arbitrage triangle (USD → EUR → GBP → USD) and
 *    multi-hop optima — that's what makes the DS&A showcase interesting.
 *    Frankfurter is strictly arbitrage-free and yields a hub-and-spoke
 *    graph (every conversion is 1 or 2 hops). Useful for proving the swap
 *    works; less useful for showing what the algorithms can do.
 *
 * 2. **Decorator in action.** The `ExchangeRatesRepository` exposed to the
 *    rest of the app is *not* the raw `DefaultExchangeRatesRepository`, it's
 *    a `CachingExchangeRatesRepository` wrapping it. The wrapped object is
 *    indistinguishable from the bare one through the interface — but every
 *    call goes through the caching layer first. That's textbook Decorator
 *    (GoF): composition over inheritance, behaviour stacked transparently.
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class DataModule {

    // Flip to `RemoteExchangeRatesDataSource` to run against Frankfurter.
    @Binds
    @Singleton
    abstract fun bindDataSource(
        impl: EmbeddedExchangeRatesDataSource,
    ): ExchangeRatesDataSource

    companion object {
        @Provides
        @Singleton
        fun provideRepository(
            inner: DefaultExchangeRatesRepository,
        ): ExchangeRatesRepository = CachingExchangeRatesRepository(inner)
    }
}
