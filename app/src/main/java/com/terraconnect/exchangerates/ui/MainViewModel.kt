package com.terraconnect.exchangerates.ui

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.terraconnect.exchangerates.data.remote.RatesApi
import kotlinx.coroutines.launch

class MainViewModel : ViewModel() {

    // The internal MutableLiveData String that stores the most recent response
    private val _response = MutableLiveData<String>()

    // The external immutable LiveData for the response String
    val response: LiveData<String>
        get() = _response

    init {
        getRates()
    }

    private fun getRates() {
        viewModelScope.launch {
            try {
                val objectResultResponse = RatesApi.retrofitService.getRates()
                _response.value =
                    "Success: ${objectResultResponse.rates.size} RatesDTO received: \n" +
                            "${objectResultResponse.rates} \n\n" +
                            "And ${objectResultResponse.pairs.size} PairsDTO received: \n" +
                            objectResultResponse.pairs.toString()
            } catch (e: Exception) {
                _response.value = "Failure: ${e.message}"
            }
        }
    }
}