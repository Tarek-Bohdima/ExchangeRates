package com.tarek.exchangerates.di

import com.squareup.moshi.Moshi
import com.tarek.exchangerates.data.remote.RatesApiService
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import javax.inject.Singleton

/**
 * Network plumbing — only exercised if you flip `DataModule` to bind
 * `RemoteExchangeRatesDataSource`. Kept here so the wiring is obvious and
 * "one switch" away.
 *
 * The backend is Frankfurter (https://www.frankfurter.dev/) — free, no API key,
 * ECB-backed daily reference rates. Trade-off vs. the embedded dataset: every
 * Frankfurter response is one base → N quotes, which yields a strictly
 * arbitrage-free hub-and-spoke graph. The algorithm showcase loses some of its
 * teeth (no arbitrage triangle, no multi-hop optima distinct from BFS), which
 * is why `DataModule` keeps `Embedded` as the default.
 */
@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    private const val BASE_URL = "https://api.frankfurter.dev/"

    @Provides
    @Singleton
    fun provideMoshi(): Moshi = Moshi.Builder().build()

    @Provides
    @Singleton
    fun provideOkHttp(): OkHttpClient = OkHttpClient.Builder()
        .addInterceptor(HttpLoggingInterceptor().apply { level = HttpLoggingInterceptor.Level.BASIC })
        .build()

    @Provides
    @Singleton
    fun provideRetrofit(client: OkHttpClient, moshi: Moshi): Retrofit = Retrofit.Builder()
        .baseUrl(BASE_URL)
        .client(client)
        .addConverterFactory(MoshiConverterFactory.create(moshi))
        .build()

    @Provides
    @Singleton
    fun provideRatesApiService(retrofit: Retrofit): RatesApiService =
        retrofit.create(RatesApiService::class.java)
}
