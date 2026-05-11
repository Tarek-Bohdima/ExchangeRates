package com.terraconnect.exchangerates.di

import com.terraconnect.exchangerates.data.repository.CachingExchangeRatesRepository
import com.terraconnect.exchangerates.data.repository.DefaultExchangeRatesRepository
import com.terraconnect.exchangerates.data.repository.ExchangeRatesRepository
import com.terraconnect.exchangerates.data.source.EmbeddedExchangeRatesDataSource
import com.terraconnect.exchangerates.data.source.ExchangeRatesDataSource
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
 *    implementation. To run against a real backend, change the binding target
 *    to `RemoteExchangeRatesDataSource` — *that's the entire change*. The
 *    repository, use cases, ViewModel, and UI are completely insulated from
 *    where the rates come from. That's the payoff of coding to interfaces.
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
