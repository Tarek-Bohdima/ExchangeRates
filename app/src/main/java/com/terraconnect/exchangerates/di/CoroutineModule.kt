package com.terraconnect.exchangerates.di

import com.terraconnect.exchangerates.core.concurrent.DispatcherProvider
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import javax.inject.Singleton

/**
 * Production dispatchers behind the [DispatcherProvider] indirection.
 * Tests provide their own via `StandardTestDispatcher` without touching
 * production code.
 */
@Module
@InstallIn(SingletonComponent::class)
object CoroutineModule {

    @Provides
    @Singleton
    fun provideDispatcherProvider(): DispatcherProvider = object : DispatcherProvider {
        override val main: CoroutineDispatcher get() = Dispatchers.Main
        override val default: CoroutineDispatcher get() = Dispatchers.Default
        override val io: CoroutineDispatcher get() = Dispatchers.IO
        override val unconfined: CoroutineDispatcher get() = Dispatchers.Unconfined
    }
}
