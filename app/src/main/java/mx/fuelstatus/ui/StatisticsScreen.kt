package mx.fuelstatus.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import mx.fuelstatus.FuelState
import mx.fuelstatus.R
import mx.fuelstatus.domain.*
import kotlin.math.abs
import kotlin.math.round

@Composable
internal fun StatisticsScreen(state: FuelState, calendar: RefuelCalendar = rememberRefuelCalendar()) {
    val stats = remember(state.selectedRefuels) { statistics(state.selectedRefuels) }
    val forecast = remember(state.selectedRefuels, state.selectedExpenses, calendar) {
        projections(state.selectedRefuels, state.selectedExpenses, calendar.today, calendar.zone)
    }
    LazyColumn(contentPadding = PaddingValues(start = 20.dp, end = 20.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item { Hero(if (state.selectedVehicle?.name == "Mazda 3") "ASÍ SE MUEVE TU MAZDA" else "ASÍ SE MUEVE TU AUTO",
            stats.kmPerLiter?.let { number(it) } ?: "—", "km/L", "Rendimiento ponderado entre tanques llenos", R.drawable.ic_speed,
            headerAction = { StatisticsInfoButton(StatisticInfo.Efficiency) }) }
        item { ConsumptionTrendCard(stats.trend) }
        projectionCards(forecast, calendar)
        item { StatisticsPair(
            { modifier -> StatisticsMetric(StatisticInfo.AverageDistance, stats.averageDistanceBetweenRefuels?.let { "${number(it, 1)} km" } ?: "—",
                R.drawable.ic_route, modifier, if (stats.refuelIntervals.isEmpty()) "Necesitas dos cargas consecutivas válidas." else "Incluye las cargas parciales.") },
            { modifier -> StatisticsMetric(StatisticInfo.Frequency, stats.averageDaysBetweenRefuels?.let {
                when (it) { 0.0 -> "El mismo día"; 1.0 -> "Cada 1 día"; else -> "Cada ${number(it, 1)} días" }
            } ?: "—",
                R.drawable.ic_category_calendar_month, modifier, if (stats.refuelIntervals.isEmpty()) "Necesitas dos cargas consecutivas válidas."
                else if (stats.minimumDaysBetweenRefuels == stats.maximumDaysBetweenRefuels)
                    "${stats.minimumDaysBetweenRefuels} ${if (stats.minimumDaysBetweenRefuels == 1L) "día" else "días"} entre cargas."
                else "Entre ${stats.minimumDaysBetweenRefuels} y ${stats.maximumDaysBetweenRefuels} días entre cargas.") }) }
        item { StatisticsPair(
            { StatisticsMetric(StatisticInfo.CostPerKm, stats.costPerKm?.let(::money) ?: "—", R.drawable.ic_payments, it) },
            { StatisticsMetric(StatisticInfo.AveragePrice, stats.averagePrice?.let(::money) ?: "—", R.drawable.ic_category_sell, it) }) }
        item { StatisticsPair(
            { StatisticsMetric(StatisticInfo.RegisteredLiters, "${number(stats.liters)} L", R.drawable.ic_water_drop, it) },
            { StatisticsMetric(StatisticInfo.EvaluatedDistance, number(stats.distanceKm, 0), R.drawable.ic_car, it) }) }
        item { StatisticsMetric(StatisticInfo.TotalSpending, money(stats.fuelCost + state.selectedExpenses.sumOf { it.costCents } / 100.0),
            R.drawable.ic_receipt, Modifier.fillMaxWidth()) }
        if (stats.periods.isNotEmpty()) item {
            val recent = stats.periods.takeLast(8)
            val max = recent.maxOf { it.kmPerLiter }.coerceAtLeast(1.0)
            Card(shape = RoundedCornerShape(24.dp)) {
                Column(Modifier.fillMaxWidth().padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    StatisticsCardHeader(StatisticInfo.RecentIntervals, R.drawable.ic_chart)
                    Text("Rendimiento en km/L", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    recent.forEach { period ->
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(date(period.endTimestamp), style = MaterialTheme.typography.labelMedium, modifier = Modifier.weight(1f))
                            Text(number(period.kmPerLiter), style = MaterialTheme.typography.labelLarge)
                        }
                        LinearProgressIndicator(progress = { (period.kmPerLiter / max).toFloat() },
                            modifier = Modifier.fillMaxWidth().height(8.dp).clip(CircleShape))
                    }
                }
            }
        }
    }
}

@Composable
private fun ConsumptionTrendCard(trend: ConsumptionTrend?) {
    Card(shape = RoundedCornerShape(24.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)) {
        Column(Modifier.fillMaxWidth().padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            StatisticsCardHeader(StatisticInfo.ConsumptionTrend, R.drawable.ic_chart)
            if (trend == null) {
                Text("Aún no hay suficientes datos", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text("Necesitas seis intervalos completos entre tanques llenos, sin cargas faltantes o excluidas entre ellos.",
                    style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            } else {
                val title = when (trend.status) {
                    ConsumptionTrendStatus.Improving -> "Consumo en mejora"
                    ConsumptionTrendStatus.Stable -> "Consumo estable"
                    ConsumptionTrendStatus.Increasing -> "Consumo en aumento"
                    ConsumptionTrendStatus.MarkedIncrease -> "Aumento importante del consumo"
                }
                val color = when (trend.status) {
                    ConsumptionTrendStatus.MarkedIncrease -> MaterialTheme.colorScheme.error
                    ConsumptionTrendStatus.Increasing -> MaterialTheme.colorScheme.tertiary
                    else -> MaterialTheme.colorScheme.primary
                }
                Text(title, color = color, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                val previousEfficiency = number(trend.previous.kmPerLiter, 1)
                val recentEfficiency = number(trend.recent.kmPerLiter, 1)
                val performance = if (previousEfficiency == recentEfficiency) "tu rendimiento se mantuvo en $recentEfficiency km/L"
                    else "tu rendimiento ${if (trend.recent.kmPerLiter < trend.previous.kmPerLiter) "bajó" else "subió"} de $previousEfficiency a $recentEfficiency km/L"
                Text(if (trend.status == ConsumptionTrendStatus.Stable)
                    "Tu consumo se mantiene similar al periodo anterior (variación: ${if (trend.changePercent > 0) "+" else ""}${number(trend.changePercent, 1)} %; $performance)."
                else "Tu consumo ${if (trend.changePercent > 0) "aumentó" else "disminuyó"} un ${number(abs(trend.changePercent), 1)} % ($performance) respecto al periodo anterior.",
                    style = MaterialTheme.typography.bodyMedium)
                val recentDistance = number(trend.recent.distanceKm, 1)
                val litersDifference = round(trend.equivalentLitersDifference * 10) / 10
                Text(if (litersDifference == 0.0)
                    "En los $recentDistance km del periodo reciente, la diferencia estimada de combustible es menor a 0.1 L respecto a mantener el consumo anterior."
                else "En los $recentDistance km del periodo reciente, equivale a unos ${number(abs(litersDifference), 1)} L ${if (litersDifference > 0) "adicionales" else "menos"} respecto a mantener el consumo anterior.",
                    style = MaterialTheme.typography.bodyMedium)
                HorizontalDivider()
                SettingsTitle(R.drawable.ic_route, "Distancia recorrida", style = MaterialTheme.typography.titleSmall)
                val distanceDifference = round(trend.distanceDifferenceKm * 10) / 10
                Text(if (distanceDifference == 0.0) "Recorriste prácticamente la misma distancia."
                    else "Recorriste ${number(abs(distanceDifference), 1)} km ${if (distanceDifference > 0) "más" else "menos"}.",
                    style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text("$recentDistance km frente a ${number(trend.previous.distanceKm, 1)} km en el periodo anterior" +
                    if (distanceDifference == 0.0) "." else ": un ${number(abs(trend.distanceChangePercent), 1)} % ${if (distanceDifference > 0) "más" else "menos"}.",
                    style = MaterialTheme.typography.bodyMedium)
                Text("En los mismos periodos de la tendencia; pueden tener distinta duración.",
                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                HorizontalDivider()
                TrendDates("Periodo reciente", trend.recent)
                TrendDates("Periodo anterior", trend.previous)
            }
        }
    }
}

@Composable
private fun TrendDates(label: String, window: ConsumptionWindow) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text("${date(window.startTimestamp)} — ${date(window.endTimestamp)}", style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
internal fun StatisticsPair(first: @Composable (Modifier) -> Unit, second: @Composable (Modifier) -> Unit) {
    val largeText = LocalDensity.current.fontScale > 1.15f
    BoxWithConstraints(Modifier.fillMaxWidth()) {
        if (maxWidth < 340.dp || largeText) Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            first(Modifier.fillMaxWidth()); second(Modifier.fillMaxWidth())
        } else CompositionLocalProvider(LocalCompactStatisticsHeader provides true) {
            Row(Modifier.fillMaxWidth().height(IntrinsicSize.Min), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                first(Modifier.weight(1f).fillMaxHeight()); second(Modifier.weight(1f).fillMaxHeight())
            }
        }
    }
}

@Composable
private fun StatisticsMetric(info: StatisticInfo, value: String, icon: Int, modifier: Modifier = Modifier, detail: String? = null) {
    Surface(modifier, shape = RoundedCornerShape(24.dp), color = MaterialTheme.colorScheme.surfaceContainerLow) {
        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            StatisticsCardHeader(info, icon, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(value, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            detail?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
        }
    }
}
