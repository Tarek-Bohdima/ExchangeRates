package com.terraconnect.exchangerates.ui.conversion

import com.terraconnect.exchangerates.core.algorithm.StrategyKind
import com.terraconnect.exchangerates.domain.model.ArbitrageOpportunity
import com.terraconnect.exchangerates.domain.model.ConversionResult
import com.terraconnect.exchangerates.domain.model.Currency

/**
 * UI state for the conversion screen — modelled as a sealed interface so the
 * Compose layer exhaustively renders every case (loading, ready, error).
 */
sealed interface ConversionUiState {

    data object Loading : ConversionUiState

    data class Error(val message: String) : ConversionUiState

    data class Ready(
        val availableCurrencies: List<Currency>,
        val source: Currency,
        val target: Currency,
        val amountInput: String,
        val selectedAlgorithm: StrategyKind,
        val supportedAlgorithms: List<StrategyKind>,
        val lastResult: ConversionResult? = null,
        val arbitrage: List<ArbitrageOpportunity> = emptyList(),
        val reachableFromSource: Set<Currency> = emptySet(),
    ) : ConversionUiState
}

/**
 * Discrete events the UI can fire at the ViewModel — keeps the public surface
 * a single function (`onEvent`) rather than a dozen named callbacks.
 */
sealed interface ConversionEvent {
    data class AmountChanged(val text: String) : ConversionEvent
    data class SourceChanged(val currency: Currency) : ConversionEvent
    data class TargetChanged(val currency: Currency) : ConversionEvent
    data class AlgorithmChanged(val kind: StrategyKind) : ConversionEvent
    data object Convert : ConversionEvent
    data object Refresh : ConversionEvent
    data object SwapCurrencies : ConversionEvent
}
