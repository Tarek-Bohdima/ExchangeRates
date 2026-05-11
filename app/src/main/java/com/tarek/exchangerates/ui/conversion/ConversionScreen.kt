package com.tarek.exchangerates.ui.conversion

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tarek.exchangerates.core.algorithm.StrategyKind
import com.tarek.exchangerates.domain.model.ArbitrageOpportunity
import com.tarek.exchangerates.domain.model.ConversionPath
import com.tarek.exchangerates.domain.model.ConversionResult
import com.tarek.exchangerates.domain.model.Currency
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConversionScreen(
    viewModel: ConversionViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Exchange Rates", fontWeight = FontWeight.SemiBold) },
                actions = {
                    IconButton(onClick = { viewModel.onEvent(ConversionEvent.Refresh) }) {
                        Icon(Icons.Filled.Refresh, contentDescription = "Refresh rates")
                    }
                },
            )
        },
    ) { padding ->
        ConversionScreenBody(
            state = state,
            onEvent = viewModel::onEvent,
            padding = padding,
        )
    }
}

@Composable
private fun ConversionScreenBody(
    state: ConversionUiState,
    onEvent: (ConversionEvent) -> Unit,
    padding: PaddingValues,
) {
    Box(modifier = Modifier.fillMaxSize().padding(padding)) {
        when (state) {
            is ConversionUiState.Loading -> CenteredLoader()
            is ConversionUiState.Error -> CenteredError(state.message)
            is ConversionUiState.Ready -> ReadyContent(state = state, onEvent = onEvent)
        }
    }
}

@Composable
private fun CenteredLoader() {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator()
    }
}

@Composable
private fun CenteredError(message: String) {
    Box(Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
        Text(
            text = message,
            color = MaterialTheme.colorScheme.error,
            style = MaterialTheme.typography.bodyLarge,
        )
    }
}

@Composable
private fun ReadyContent(
    state: ConversionUiState.Ready,
    onEvent: (ConversionEvent) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        ConversionInputCard(state = state, onEvent = onEvent)
        AlgorithmPickerCard(state = state, onEvent = onEvent)
        state.lastResult?.let { ResultCard(result = it, algorithm = state.selectedAlgorithm) }
        if (state.arbitrage.isNotEmpty()) ArbitrageCard(opportunities = state.arbitrage)
        if (state.reachableFromSource.isNotEmpty()) {
            ReachableCard(state.source, state.reachableFromSource)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ConversionInputCard(
    state: ConversionUiState.Ready,
    onEvent: (ConversionEvent) -> Unit,
) {
    Card(elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("Convert", style = MaterialTheme.typography.titleLarge)
            OutlinedTextField(
                value = state.amountInput,
                onValueChange = { onEvent(ConversionEvent.AmountChanged(it)) },
                label = { Text("Amount") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.fillMaxWidth(),
            )
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                CurrencyDropdown(
                    label = "From",
                    selected = state.source,
                    options = state.availableCurrencies,
                    onSelected = { onEvent(ConversionEvent.SourceChanged(it)) },
                    modifier = Modifier.weight(1f),
                )
                FilledIconButton(onClick = { onEvent(ConversionEvent.SwapCurrencies) }) {
                    Icon(Icons.Filled.SwapHoriz, contentDescription = "Swap currencies")
                }
                CurrencyDropdown(
                    label = "To",
                    selected = state.target,
                    options = state.availableCurrencies,
                    onSelected = { onEvent(ConversionEvent.TargetChanged(it)) },
                    modifier = Modifier.weight(1f),
                )
            }
            Button(
                onClick = { onEvent(ConversionEvent.Convert) },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Convert")
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CurrencyDropdown(
    label: String,
    selected: Currency,
    options: List<Currency>,
    onSelected: (Currency) -> Unit,
    modifier: Modifier = Modifier,
) {
    var expanded by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = it },
        modifier = modifier,
    ) {
        OutlinedTextField(
            value = selected.code,
            onValueChange = {},
            readOnly = true,
            label = { Text(label) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier
                .fillMaxWidth()
                .menuAnchor(),
        )
        androidx.compose.material3.ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
        ) {
            options.forEach { currency ->
                DropdownMenuItem(
                    text = { Text(currency.code) },
                    onClick = {
                        onSelected(currency)
                        expanded = false
                    },
                )
            }
        }
    }
}

@Composable
private fun AlgorithmPickerCard(
    state: ConversionUiState.Ready,
    onEvent: (ConversionEvent) -> Unit,
) {
    Card(elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Algorithm", style = MaterialTheme.typography.titleLarge)
            Text(
                state.selectedAlgorithm.description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                "Complexity: ${state.selectedAlgorithm.complexity}",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            // Filter chips give a "tabby" picker without pulling in a TabRow.
            // Horizontal scroll handles overflow on narrow screens.
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                state.supportedAlgorithms.forEach { kind ->
                    FilterChip(
                        selected = kind == state.selectedAlgorithm,
                        onClick = { onEvent(ConversionEvent.AlgorithmChanged(kind)) },
                        label = { Text(kind.displayName) },
                    )
                }
            }
        }
    }
}

@Composable
private fun ResultCard(
    result: ConversionResult,
    algorithm: StrategyKind,
) {
    Card(elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Result", style = MaterialTheme.typography.titleLarge)
            when (result) {
                is ConversionResult.Success -> SuccessResult(result, algorithm)
                is ConversionResult.Unreachable -> Text(
                    "No conversion path between ${result.from} and ${result.to}.",
                    color = MaterialTheme.colorScheme.error,
                )
                is ConversionResult.UnknownCurrency -> Text(
                    "Unknown currency: ${result.currency}",
                    color = MaterialTheme.colorScheme.error,
                )
                is ConversionResult.Failure -> Text(
                    "Conversion failed: ${result.reason}",
                    color = MaterialTheme.colorScheme.error,
                )
            }
        }
    }
}

@Composable
private fun SuccessResult(
    result: ConversionResult.Success,
    algorithm: StrategyKind,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = result.output.rounded().toString(),
            style = MaterialTheme.typography.displayMedium,
            color = MaterialTheme.colorScheme.primary,
        )
        Text(
            text = "Composite rate: ${String.format(Locale.US, "%.6f", result.rate)}",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = "Hops: ${result.path.hops}  •  ${algorithm.displayName}",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        PathBreadcrumb(result.path)
    }
}

@Composable
private fun PathBreadcrumb(path: ConversionPath) {
    if (path.isIdentity) {
        Text("(identity — no conversion needed)", style = MaterialTheme.typography.bodyMedium)
        return
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        path.visited.forEachIndexed { index, currency ->
            CurrencyChip(currency)
            if (index < path.visited.lastIndex) {
                Text(
                    "→",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun CurrencyChip(currency: Currency) {
    Box(
        modifier = Modifier
            .background(
                color = MaterialTheme.colorScheme.secondaryContainer,
                shape = RoundedCornerShape(8.dp),
            )
            .padding(horizontal = 10.dp, vertical = 6.dp),
    ) {
        Text(
            text = currency.code,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSecondaryContainer,
        )
    }
}

@Composable
private fun ArbitrageCard(opportunities: List<ArbitrageOpportunity>) {
    Card(
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Filled.TrendingUp,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onTertiaryContainer,
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    "Arbitrage opportunities (Bellman-Ford)",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onTertiaryContainer,
                )
            }
            opportunities.forEach { opp ->
                Column {
                    Text(
                        text = opp.cycle.joinToString(" → ") { it.code },
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onTertiaryContainer,
                    )
                    Text(
                        text = "Profit: ${String.format(Locale.US, "%+.3f", opp.profitPercent)}%",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onTertiaryContainer,
                    )
                }
            }
        }
    }
}

@Composable
private fun ReachableCard(source: Currency, reachable: Set<Currency>) {
    Card(elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                "Reachable from ${source.code} (Union-Find)",
                style = MaterialTheme.typography.titleMedium,
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                reachable.sorted().forEach { CurrencyChip(it) }
            }
        }
    }
}
