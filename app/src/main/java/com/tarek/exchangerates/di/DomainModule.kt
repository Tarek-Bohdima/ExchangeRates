package com.tarek.exchangerates.di

import com.tarek.exchangerates.domain.usecase.ConversionFacade
import com.tarek.exchangerates.domain.usecase.DefaultConversionFacade
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Domain-layer Hilt module — binds the facade interface to its default
 * implementation. Consumers (the ViewModel, tests) only ever see the
 * [ConversionFacade] interface.
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class DomainModule {

    @Binds
    @Singleton
    abstract fun bindConversionFacade(impl: DefaultConversionFacade): ConversionFacade
}
