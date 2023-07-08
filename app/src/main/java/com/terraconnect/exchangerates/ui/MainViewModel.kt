package com.terraconnect.exchangerates.ui

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.terraconnect.exchangerates.data.Result
import com.terraconnect.exchangerates.models.Rate
import com.terraconnect.exchangerates.repository.BaseRepository
import com.terraconnect.exchangerates.util.DispatcherProvider
import com.terraconnect.exchangerates.util.RateCalculator
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    private val repository: BaseRepository,
    private val dispatchers: DispatcherProvider,
) : ViewModel() {

    private val _resultRates = MutableLiveData<List<Rate>>()
    val resultRates: LiveData<List<Rate>>
        get() = _resultRates

    private fun convert() {
        viewModelScope.launch(dispatchers.main) {
            when (val ratesResponse = repository.getExchangeRates()) {
                is Result.Success -> {
                    val rates = ratesResponse.data.rates
                    val pairs = ratesResponse.data.pairs

                    val calculator = RateCalculator(rates)

                    val calculatedRates = pairs.mapNotNull { pair ->
                        val conversionRate = calculator.calculateRate(pair.from, pair.to)
                        if (conversionRate != null) {
                            Rate(pair.from, pair.to, conversionRate)
                        } else {
                            null
                        }
                    }

                    _resultRates.value = calculatedRates
                }
                else -> {}
            }
        }
    }

    init {
        convert()
    }
}
