package com.terraconnect.exchangerates.ui.conversion

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.terraconnect.exchangerates.core.algorithm.StrategyFactory
import com.terraconnect.exchangerates.core.algorithm.StrategyKind
import com.terraconnect.exchangerates.core.ds.CurrencyGraph
import com.terraconnect.exchangerates.domain.model.Currency
import com.terraconnect.exchangerates.domain.model.Money
import com.terraconnect.exchangerates.domain.usecase.ConversionFacade
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch
import timber.log.Timber
import java.math.BigDecimal
import javax.inject.Inject

/**
 * Single ViewModel for the conversion screen.
 *
 * Talks to the world through one collaborator — the [ConversionFacade] —
 * which is the Law of Demeter applied to the architecture: the ViewModel
 * doesn't know there are use cases, repositories, data sources, or
 * algorithms behind the facade. It asks "convert this" or "find arbitrage"
 * and gets an answer.
 *
 * State is exposed as a [StateFlow] of a sealed [ConversionUiState] — the
 * Compose layer can `when`-switch over Loading / Ready / Error exhaustively.
 *
 * Input goes through a single [onEvent] sink so the UI's public surface is
 * minimal (one function reference). That keeps Composables simple to test.
 */
@HiltViewModel
class ConversionViewModel @Inject constructor(
    private val facade: ConversionFacade,
    private val strategyFactory: StrategyFactory,
) : ViewModel() {

    private var graph: CurrencyGraph? = null

    private val _uiState = MutableStateFlow<ConversionUiState>(ConversionUiState.Loading)
    val uiState: StateFlow<ConversionUiState> = _uiState.asStateFlow()

    init {
        // Observe the graph; recompute derived data (arbitrage, reachability)
        // each time it changes.
        viewModelScope.launch {
            facade.observeGraph()
                .catch { e ->
                    Timber.e(e, "Failed to load rates")
                    _uiState.value = ConversionUiState.Error(
                        message = e.message ?: "Failed to load rates",
                    )
                }
                .collect { newGraph -> onGraphReady(newGraph) }
        }
    }

    fun onEvent(event: ConversionEvent) {
        val current = _uiState.value as? ConversionUiState.Ready ?: return
        when (event) {
            is ConversionEvent.AmountChanged -> _uiState.value = current.copy(amountInput = event.text)
            is ConversionEvent.SourceChanged -> {
                val updated = current.copy(
                    source = event.currency,
                    reachableFromSource = graph?.let { facade.reachableFrom(it, event.currency) } ?: emptySet(),
                    lastResult = null,
                )
                _uiState.value = updated
            }
            is ConversionEvent.TargetChanged -> _uiState.value = current.copy(target = event.currency, lastResult = null)
            is ConversionEvent.AlgorithmChanged -> _uiState.value = current.copy(selectedAlgorithm = event.kind)
            ConversionEvent.SwapCurrencies -> _uiState.value = current.copy(
                source = current.target,
                target = current.source,
                lastResult = null,
                reachableFromSource = graph?.let { facade.reachableFrom(it, current.target) } ?: emptySet(),
            )
            ConversionEvent.Convert -> runConversion(current)
            ConversionEvent.Refresh -> viewModelScope.launch {
                runCatching { facade.refresh() }
                    .onFailure { Timber.e(it, "Refresh failed") }
            }
        }
    }

    private fun onGraphReady(newGraph: CurrencyGraph) {
        graph = newGraph
        val currencies = newGraph.vertices.sorted()
        val current = _uiState.value
        val ready = if (current is ConversionUiState.Ready) {
            // Existing state — only patch fields that depend on the graph.
            current.copy(
                availableCurrencies = currencies,
                arbitrage = facade.arbitrage(newGraph),
                reachableFromSource = facade.reachableFrom(newGraph, current.source),
            )
        } else {
            // Fresh boot — pick sensible defaults.
            val source = currencies.firstOrNull { it == Currency.of("USD") } ?: currencies.first()
            val target = currencies.firstOrNull { it != source }
                ?: error("Graph has fewer than two currencies — nothing to convert")
            ConversionUiState.Ready(
                availableCurrencies = currencies,
                source = source,
                target = target,
                amountInput = DEFAULT_AMOUNT,
                selectedAlgorithm = StrategyKind.DIJKSTRA,
                supportedAlgorithms = strategyFactory.available.toList().sortedBy { it.ordinal },
                arbitrage = facade.arbitrage(newGraph),
                reachableFromSource = facade.reachableFrom(newGraph, source),
            )
        }
        _uiState.value = ready
    }

    private fun runConversion(current: ConversionUiState.Ready) {
        val graph = this.graph ?: return
        val amount = parseAmount(current.amountInput) ?: return
        val money = Money(amount = amount, currency = current.source)
        val result = facade.convert(
            graph = graph,
            input = money,
            target = current.target,
            kind = current.selectedAlgorithm,
        )
        _uiState.value = current.copy(lastResult = result)
    }

    private fun parseAmount(text: String): BigDecimal? = runCatching {
        BigDecimal(text.trim().ifEmpty { return null }).takeIf { it.signum() >= 0 }
    }.getOrNull()

    private companion object {
        private const val DEFAULT_AMOUNT = "100"
    }
}
