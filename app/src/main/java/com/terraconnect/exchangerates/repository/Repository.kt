package com.terraconnect.exchangerates.repository

import com.terraconnect.exchangerates.data.Result
import com.terraconnect.exchangerates.data.remote.RatesApiService
import com.terraconnect.exchangerates.data.remote.dto.asDomainModel
import com.terraconnect.exchangerates.models.ExchangeRates
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import javax.inject.Inject

class Repository @Inject constructor(
    private val api: RatesApiService,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
) : BaseRepository {

    override suspend fun getExchangeRates(): Result<ExchangeRates> {
        return try {
            val response = api.getRates()
            val result = response.body()
            if (response.isSuccessful && result != null) {
                Result.Success(result.asDomainModel())
            } else {
                Result.Error(Exception(response.message()))
            }
        } catch (e: Exception) {
            Result.Error(e)
        }
    }
}