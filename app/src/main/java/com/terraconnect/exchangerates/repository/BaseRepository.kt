package com.terraconnect.exchangerates.repository

import com.terraconnect.exchangerates.data.Result
import com.terraconnect.exchangerates.models.ExchangeRates

interface BaseRepository {
    suspend fun getExchangeRates(): Result<ExchangeRates>
}