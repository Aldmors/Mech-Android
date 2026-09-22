package com.mech.carexpensetracker.ui.charts

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ShowChart
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Download
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mech.carexpensetracker.R
import com.mech.carexpensetracker.domain.model.ChartDatePreset
import com.mech.carexpensetracker.domain.model.ChartKind
import com.mech.carexpensetracker.ui.components.AppLazyColumn
import com.mech.carexpensetracker.ui.components.EmptyStateCard
import com.mech.carexpensetracker.ui.components.SecondaryButton
import com.mech.carexpensetracker.ui.components.SectionHeader
import com.mech.carexpensetracker.ui.theme.DesignTokens
import com.patrykandpatrick.vico.compose.cartesian.CartesianChartHost
import com.patrykandpatrick.vico.compose.cartesian.axis.rememberBottomAxis
import com.patrykandpatrick.vico.compose.cartesian.axis.rememberStartAxis
import com.patrykandpatrick.vico.compose.cartesian.layer.rememberColumnCartesianLayer
import com.patrykandpatrick.vico.compose.cartesian.layer.rememberLineCartesianLayer
import com.patrykandpatrick.vico.compose.cartesian.rememberCartesianChart
import com.patrykandpatrick.vico.compose.cartesian.rememberVicoScrollState
import com.patrykandpatrick.vico.compose.common.ProvideVicoTheme
import com.patrykandpatrick.vico.compose.m3.common.rememberM3VicoTheme
import com.patrykandpatrick.vico.core.cartesian.Scroll
import com.patrykandpatrick.vico.core.cartesian.data.CartesianChartModelProducer
import com.patrykandpatrick.vico.core.cartesian.data.CartesianValueFormatter
import com.patrykandpatrick.vico.core.cartesian.data.columnSeries
import com.patrykandpatrick.vico.core.cartesian.data.lineSeries
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.Locale

private enum class ChartRenderStyle { Column, Line }

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ChartsScreen(
    onMessage: (String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ChartsViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val kind = state.chartKind
    val preset = state.preset
    var style by rememberSaveable { mutableStateOf(ChartRenderStyle.Line) }
    var chartBounds by remember { mutableStateOf<Rect?>(null) }
    val savedMessage = stringResource(R.string.chart_saved)
    val failedMessage = stringResource(R.string.chart_save_failed)

    fun saveChart() {
        val bounds = chartBounds
        val window = ChartImageSaver.findActivity(context)?.window
        if (bounds == null || window == null) {
            onMessage(failedMessage)
            return
        }
        scope.launch {
            val bitmap = ChartImageSaver.capture(window, bounds)
            if (bitmap == null) {
                onMessage(failedMessage)
                return@launch
            }
            try {
                val saved = withContext(Dispatchers.IO) { ChartImageSaver.savePng(context, bitmap) }
                onMessage(if (saved) savedMessage else failedMessage)
            } finally {
                bitmap.recycle()
            }
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted ->
        if (granted) saveChart() else onMessage(failedMessage)
    }

    AppLazyColumn(modifier = modifier) {
        item { SectionHeader(title = stringResource(R.string.chart_type)) }
        item {
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(DesignTokens.Spacing.sm),
                verticalArrangement = Arrangement.spacedBy(DesignTokens.Spacing.sm),
            ) {
                ChartKind.entries.forEach { chartKind ->
                    FilterChip(
                        selected = kind == chartKind,
                        onClick = {
                            style = defaultRenderStyle(chartKind)
                            viewModel.load(chartKind, preset)
                        },
                        label = { Text(stringResource(chartKindStringRes(chartKind))) },
                    )
                }
            }
        }
        item { SectionHeader(title = stringResource(R.string.chart_style)) }
        item {
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(DesignTokens.Spacing.sm),
                verticalArrangement = Arrangement.spacedBy(DesignTokens.Spacing.sm),
            ) {
                ChartRenderStyle.entries.forEach { renderStyle ->
                    FilterChip(
                        selected = style == renderStyle,
                        onClick = { style = renderStyle },
                        leadingIcon = {
                            Icon(
                                imageVector = renderStyle.icon,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp),
                            )
                        },
                        label = { Text(stringResource(renderStyle.labelRes)) },
                    )
                }
            }
        }
        item { SectionHeader(title = stringResource(R.string.chart_period)) }
        item {
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(DesignTokens.Spacing.sm),
                verticalArrangement = Arrangement.spacedBy(DesignTokens.Spacing.sm),
            ) {
                ChartDatePreset.entries.forEach { datePreset ->
                    FilterChip(
                        selected = preset == datePreset,
                        onClick = { viewModel.load(kind, datePreset) },
                        label = { Text(stringResource(chartPresetStringRes(datePreset))) },
                    )
                }
            }
        }
        item {
            if (state.hasData) {
                Column(verticalArrangement = Arrangement.spacedBy(DesignTokens.Spacing.sm)) {
                    ChartContent(
                        state = state,
                        style = style,
                        description = stringResource(chartKindStringRes(kind)),
                        onBounds = { chartBounds = it },
                    )
                    SecondaryButton(
                        text = stringResource(R.string.download_chart),
                        onClick = {
                            val needsPermission = Build.VERSION.SDK_INT < Build.VERSION_CODES.Q &&
                                ContextCompat.checkSelfPermission(
                                    context,
                                    Manifest.permission.WRITE_EXTERNAL_STORAGE,
                                ) != PackageManager.PERMISSION_GRANTED
                            if (needsPermission) {
                                permissionLauncher.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                            } else {
                                saveChart()
                            }
                        },
                        icon = Icons.Default.Download,
                    )
                }
            } else {
                EmptyStateCard(message = stringResource(R.string.no_data))
            }
        }
    }
}

@Composable
private fun ChartContent(
    state: ChartsUiState,
    style: ChartRenderStyle,
    description: String,
    onBounds: (Rect) -> Unit,
) {
    val modelProducer = remember(style, state.chartKind, state.preset) { CartesianChartModelProducer() }
    val axisLabels = remember(state.axisLabels, state.chartKind, state.preset) {
        chartAxisLabels(state.axisLabels, state.chartKind, state.preset)
    }
    val axisValueFormatter = remember(axisLabels) {
        CartesianValueFormatter { value, _, _ -> axisLabels.getOrElse(value.toInt()) { "" } }
    }
    val scrollState = rememberVicoScrollState(initialScroll = Scroll.Absolute.Start)
    val chartModifier = Modifier
        .padding(vertical = DesignTokens.Spacing.sm)
        .semantics { contentDescription = description }

    LaunchedEffect(style, state.chartKind, state.fuelValues, state.repairValues, state.papersValues, state.careValues, state.lineValues) {
        val monthly = state.chartKind == ChartKind.MonthlySpending
        modelProducer.runTransaction {
            if (style == ChartRenderStyle.Column) {
                columnSeries {
                    if (monthly) {
                        series(state.fuelValues)
                        series(state.repairValues)
                        series(state.papersValues)
                        series(state.careValues)
                    } else {
                        series(state.lineValues)
                    }
                }
            } else {
                lineSeries {
                    if (monthly) {
                        series(state.fuelValues)
                        series(state.repairValues)
                        series(state.papersValues)
                        series(state.careValues)
                    } else {
                        series(state.lineValues)
                    }
                }
            }
        }
    }

    val seriesColors = DesignTokens.Palette.run { listOf(fuel, repair, papers, care) }
    ProvideVicoTheme(
        rememberM3VicoTheme(
            columnCartesianLayerColors = seriesColors,
            lineCartesianLayerColors = seriesColors,
        ),
    ) {
        Column(
            modifier = Modifier.onGloballyPositioned { onBounds(it.boundsInWindow()) },
            verticalArrangement = Arrangement.spacedBy(DesignTokens.Spacing.sm),
        ) {
            if (state.chartKind == ChartKind.MonthlySpending) {
                ChartLegend()
            }
            key(state.chartKind, state.preset, style) {
                when (style) {
                    ChartRenderStyle.Column -> {
                        CartesianChartHost(
                            chart = rememberCartesianChart(
                                rememberColumnCartesianLayer(),
                                startAxis = rememberStartAxis(),
                                bottomAxis = rememberBottomAxis(valueFormatter = axisValueFormatter),
                            ),
                            modelProducer = modelProducer,
                            scrollState = scrollState,
                            modifier = chartModifier,
                        )
                    }
                    ChartRenderStyle.Line -> {
                        CartesianChartHost(
                            chart = rememberCartesianChart(
                                rememberLineCartesianLayer(),
                                startAxis = rememberStartAxis(),
                                bottomAxis = rememberBottomAxis(valueFormatter = axisValueFormatter),
                            ),
                            modelProducer = modelProducer,
                            scrollState = scrollState,
                            modifier = chartModifier,
                        )
                    }
                }
            }
        }
    }
}

@Composable
@OptIn(ExperimentalLayoutApi::class)
private fun ChartLegend() {
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(DesignTokens.Spacing.md),
        verticalArrangement = Arrangement.spacedBy(DesignTokens.Spacing.sm),
    ) {
        LegendItem(DesignTokens.Palette.fuel, stringResource(R.string.event_type_fuel))
        LegendItem(DesignTokens.Palette.repair, stringResource(R.string.event_type_service))
        LegendItem(DesignTokens.Palette.papers, stringResource(R.string.event_type_documents))
        LegendItem(DesignTokens.Palette.care, stringResource(R.string.event_type_care))
    }
}

@Composable
private fun LegendItem(color: Color, label: String) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(DesignTokens.Spacing.xs),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Surface(
            modifier = Modifier.size(12.dp),
            shape = CircleShape,
            color = color,
            content = {},
        )
        Text(text = label, style = MaterialTheme.typography.labelMedium)
    }
}

private fun defaultRenderStyle(kind: ChartKind): ChartRenderStyle = when (kind) {
    ChartKind.FuelConsumption, ChartKind.CumulativeCost, ChartKind.MonthlySpending -> ChartRenderStyle.Line
    ChartKind.CategoryBreakdown -> ChartRenderStyle.Column
}

private val ChartRenderStyle.icon: ImageVector
    get() = when (this) {
        ChartRenderStyle.Column -> Icons.Default.BarChart
        ChartRenderStyle.Line -> Icons.AutoMirrored.Filled.ShowChart
    }

@get:StringRes
private val ChartRenderStyle.labelRes: Int
    get() = when (this) {
        ChartRenderStyle.Column -> R.string.chart_column
        ChartRenderStyle.Line -> R.string.chart_line
    }

@StringRes
private fun chartKindStringRes(kind: ChartKind): Int = when (kind) {
    ChartKind.MonthlySpending -> R.string.chart_monthly_spending
    ChartKind.FuelConsumption -> R.string.chart_fuel_consumption
    ChartKind.CategoryBreakdown -> R.string.chart_category_breakdown
    ChartKind.CumulativeCost -> R.string.chart_cumulative_cost
}

@StringRes
private fun chartPresetStringRes(preset: ChartDatePreset): Int = when (preset) {
    ChartDatePreset.ThreeMonths -> R.string.chart_three_months
    ChartDatePreset.SixMonths -> R.string.chart_six_months
    ChartDatePreset.TwelveMonths -> R.string.chart_twelve_months
    ChartDatePreset.AllTime -> R.string.chart_all_time
}

private val yearMonthLabel = Regex("""^\d{4}-\d{2}$""")

internal fun chartAxisLabels(
    labels: List<String>,
    kind: ChartKind,
    preset: ChartDatePreset,
    locale: Locale = Locale.getDefault(),
): List<String> {
    if (kind == ChartKind.CategoryBreakdown) return labels

    val months = labels.map { label ->
        label.takeIf { yearMonthLabel.matches(it) }
            ?.let { runCatching { YearMonth.parse(it) }.getOrNull() }
    }
    val includeYear = preset == ChartDatePreset.AllTime ||
        months.filterNotNull().map { it.year }.distinct().size > 1
    val formatter = DateTimeFormatter.ofPattern(if (includeYear) "MMM yyyy" else "MMM", locale)
    return labels.zip(months) { label, month -> month?.format(formatter) ?: label }
}
