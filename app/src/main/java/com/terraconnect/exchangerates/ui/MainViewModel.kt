package com.terraconnect.exchangerates.ui

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.terraconnect.exchangerates.data.Result
import com.terraconnect.exchangerates.models.Pairs
import com.terraconnect.exchangerates.models.Rates
import com.terraconnect.exchangerates.repository.BaseRepository
import com.terraconnect.exchangerates.util.DispatcherProvider
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    private val repository: BaseRepository,
    private val dispatchers: DispatcherProvider,
) : ViewModel() {

    private val _resultRates = MutableLiveData<List<Rates>>()
    val resultRates: LiveData<List<Rates>>
        get() = _resultRates


    private fun convert() {
        viewModelScope.launch(dispatchers.main) {
            when (val ratesResponse = repository.getExchangeRates()) {
                is Result.Success -> {
                    val rates = ratesResponse.data.rates
                    val pairs = ratesResponse.data.pairs

                    _resultRates.value = evaluateRate(pairs, rates)
                }
                else -> {}
            }
        }
    }

    private fun evaluateRate(
        pairs: List<Pairs>,
        rates: List<Rates>,
    ): List<Rates> {
        var from = ""
        var to = ""
        var conversionRate = 0.0
        val resultRatesList = emptyList<Rates>().toMutableList()
        for (pair in pairs) {
            for (rateObject in rates) {

                // find direct rate
                if (pair.from == rateObject.from && pair.to == rateObject.to) {
                    conversionRate = rateObject.rate
                    from = pair.from
                    to = pair.to

                    //find indirect rate
                } else if (pair.from == rateObject.from && pair.to != rateObject.to) {
                    for (rateObject2 in rates) {
                        if (rateObject.to == rateObject2.from && rateObject2.to == pair.to) {
                            conversionRate = rateObject.rate * rateObject2.rate
                            from = pair.from
                            to = pair.to

                            // find 2nd indirect rate ( 2 intermediate cross currency)
                        } else if (pair.to != rateObject2.to && rateObject2.from == rateObject.to) {
                            for (rateObject3 in rates) {
                                if (rateObject2.to == rateObject3.from && rateObject3.to == pair.to && rateObject.to == rateObject2.from) {
                                    conversionRate =
                                        rateObject.rate * rateObject2.rate * rateObject3.rate
                                    from = pair.from
                                    to = pair.to
                                }
                            }
                        }
                    }
                }
            }
            val rate = Rates(from, to, conversionRate)
            resultRatesList.add(rate)
        }

        return resultRatesList
    }

    init {
        convert()
    }
}