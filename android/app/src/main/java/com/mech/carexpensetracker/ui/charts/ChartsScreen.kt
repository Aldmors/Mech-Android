package com.mech.carexpensetracker.ui.charts

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mech.carexpensetracker.R
import com.mech.carexpensetracker.domain.model.ChartDatePreset
import com.mech.carexpensetracker.domain.model.ChartKind
import com.mech.carexpensetracker.ui.components.EmptyStateCard
import com.mech.carexpensetracker.ui.components.SectionHeader
import com.mech.carexpensetracker.ui.theme.DesignTokens
import com.patrykandpatrick.vico.compose.cartesian.CartesianChartHost
import com.patrykandpatrick.vico.compose.cartesian.axis.rememberBottomAxis
import com.patrykandpatrick.vico.compose.cartesian.axis.rememberStartAxis
import com.patrykandpatrick.vico.compose.cartesian.layer.rememberColumnCartesianLayer
import com.patrykandpatrick.vico.compose.cartesian.layer.rememberLineCartesianLayer
import com.patrykandpatrick.vico.compose.cartesian.rememberCartesianChart
import com.patrykandpatrick.vico.core.cartesian.data.CartesianChartModelProducer
import com.patrykandpatrick.vico.core.cartesian.data.columnSeries
import com.patrykandpatrick.vico.core.cartesian.data.lineSeries

@Composable
fun ChartsScreen(
    modifier: Modifier = Modifier,
    viewModel: ChartsViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var kind by remember { mutableStateOf(ChartKind.MonthlySpending) }
    var preset by remember { mutableStateOf(ChartDatePreset.TwelveMonths) }

    LazyColumn(
        modifier = modifier.fillMaxSize().padding(DesignTokens.Spacing.md),
    ) {
        item {
            SectionHeader(title = stringResource(R.string.charts))
        }
        item {
            ChartKind.entries.forEach { chartKind ->
                FilterChip(
                    selected = kind == chartKind,
                    onClick = { kind = chartKind; viewModel.load(chartKind, preset) },
                    label = { Text(chartKind.label) },
                )
            }
        }
        item {
            ChartDatePreset.entries.forEach { datePreset ->
                FilterChip(
                    selected = preset == datePreset,
                    onClick = { preset = datePreset; viewModel.load(kind, datePreset) },
                    label = { Text(datePreset.label) },
                )
            }
        }
        item {
            if (state.hasData) {
                ChartContent(state = state)
            } else {
                EmptyStateCard(message = stringResource(R.string.no_data))
            }
        }
    }
}

@Composable
private fun ChartContent(state: ChartsUiState) {
    val modelProducer = remember { CartesianChartModelProducer() }

    LaunchedEffect(state.chartKind, state.fuelValues, state.repairValues, state.papersValues, state.lineValues) {
        when (state.chartKind) {
            ChartKind.MonthlySpending -> {
                modelProducer.runTransaction {
                    columnSeries {
                        series(state.fuelValues)
                        series(state.repairValues)
                        series(state.papersValues)
                    }
                }
            }
            ChartKind.FuelConsumption, ChartKind.CumulativeCost, ChartKind.CategoryBreakdown -> {
                modelProducer.runTransaction {
                    if (state.chartKind == ChartKind.FuelConsumption || state.chartKind == ChartKind.CumulativeCost) {
                        lineSeries { series(state.lineValues) }
                    } else {
                        columnSeries { series(state.lineValues) }
                    }
                }
            }
        }
    }

    when (state.chartKind) {
        ChartKind.MonthlySpending, ChartKind.CategoryBreakdown -> {
            CartesianChartHost(
                chart = rememberCartesianChart(
                    rememberColumnCartesianLayer(),
                    startAxis = rememberStartAxis(),
                    bottomAxis = rememberBottomAxis(),
                ),
                modelProducer = modelProducer,
                modifier = Modifier.padding(DesignTokens.Spacing.md),
            )
        }
        ChartKind.FuelConsumption, ChartKind.CumulativeCost -> {
            CartesianChartHost(
                chart = rememberCartesianChart(
                    rememberLineCartesianLayer(),
                    startAxis = rememberStartAxis(),
                    bottomAxis = rememberBottomAxis(),
                ),
                modelProducer = modelProducer,
                modifier = Modifier.padding(DesignTokens.Spacing.md),
            )
        }
    }
}
