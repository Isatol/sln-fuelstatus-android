package mx.fuelstatus.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import mx.fuelstatus.R
import mx.fuelstatus.data.FuelType
import mx.fuelstatus.domain.*
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import kotlin.math.abs
import kotlin.math.round

private fun forecastDate(day: LocalDate) = day.format(DateTimeFormatter.ofPattern("d MMM yyyy", MexicanLocale))
private fun rounded(value: Double) = round(value * 10) / 10

internal fun LazyListScope.projectionCards(data: StatisticsProjections, calendar: RefuelCalendar) {
    item { NextRefuelCard(data.nextFullRefuel, data.completeIntervalCount, calendar.today) }
    item { UsualRangeCard(data.usualRange) }
    item { StatisticsPair(
        { modifier -> ProjectionCard(StatisticInfo.RecentCost, R.drawable.ic_payments,
            data.costPer100Km?.let(::money) ?: "Sin datos suficientes",
            if (data.costPer100Km == null) "Necesitas un intervalo completo válido entre tanques llenos."
                else "Costo de combustible por cada 100 km.",
            modifier = modifier, basis = data.recentThrough?.let { "${data.recentIntervalCount} intervalos completos · hasta ${forecastDate(it)}" }) },
        { modifier -> PriceTrendCard(data.priceTrend, modifier) }) }
    item { SavingsCard(data.savings) }
    item { MonthlySpendingCard(data.monthlySpending, data.hasCurrentMonthRecords, calendar.today) }
}

@Composable
private fun ProjectionCard(info: StatisticInfo, icon: Int, value: String, description: String,
    modifier: Modifier = Modifier, basis: String? = null) {
    Surface(modifier.fillMaxWidth(), shape = RoundedCornerShape(24.dp), color = MaterialTheme.colorScheme.surfaceContainerLow) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            StatisticsCardHeader(info, icon, style = MaterialTheme.typography.titleSmall)
            Text(value, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Text(description, style = MaterialTheme.typography.bodyMedium)
            basis?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
        }
    }
}

@Composable
private fun NextRefuelCard(next: NextFullRefuel?, completeCount: Int, today: LocalDate) {
    val info = StatisticInfo.NextFullRefuel
    if (next == null) {
        val missing = (3 - completeCount).coerceAtLeast(0)
        ProjectionCard(info, R.drawable.ic_category_calendar_month,
            when (missing) { 0 -> "Falta una frecuencia en días distintos"; 1 -> "Falta 1 intervalo completo"; else -> "Faltan $missing intervalos completos" },
            "Necesitas al menos tres intervalos completos válidos y una frecuencia habitual mayor a cero días.")
    } else if (next.latest < today) {
        ProjectionCard(info, R.drawable.ic_category_calendar_month, "Actualiza tu historial",
            "El rango estimado terminaba el ${forecastDate(next.latest)}. Registra tus cargas recientes para actualizar la previsión.",
            basis = "Basada en ${next.intervalCount} intervalos entre tanques llenos; no se desplaza la fecha automáticamente.")
    } else {
        val start = maxOf(next.earliest, today)
        val value = if (start == next.latest) forecastDate(start) else "${forecastDate(start)} — ${forecastDate(next.latest)}"
        val low = ChronoUnit.DAYS.between(today, start)
        val high = ChronoUnit.DAYS.between(today, next.latest)
        val wait = if (low == high) when (low) {
            0L -> "Podría ser hoy."; 1L -> "Podría ser mañana."; else -> "Dentro de unos $low días."
        } else if (low == 0L) "Entre hoy y dentro de $high días." else "Dentro de $low a $high días."
        ProjectionCard(info, R.drawable.ic_category_calendar_month, value,
            "Según tus hábitos, estimamos que tu próxima carga con tanque lleno podría estar en este rango. $wait",
            basis = "Basada en ${next.intervalCount} intervalos completos. " +
                (if (next.typical >= today) "Fecha habitual: ${forecastDate(next.typical)}. " else "") +
                if (next.partialSinceLastFull) "Hay una carga parcial posterior; la fecha sigue siendo orientativa."
                else "La fecha es orientativa y depende de cuánto conduzcas.")
    }
}

@Composable
private fun UsualRangeCard(range: UsualRefuelRange?) {
    if (range == null) {
        ProjectionCard(StatisticInfo.UsualRange, R.drawable.ic_route, "Necesitas tres intervalos completos",
            "El alcance se estimará con tus litros habituales y el rendimiento reciente.")
        return
    }
    val difference = range.previousDistanceKm?.let { rounded(range.distanceKm - it) }
    val comparison = when {
        difference == null -> "."
        difference == 0.0 -> ": un alcance similar al del periodo anterior."
        else -> ": unos ${number(abs(difference), 1)} km ${if (difference > 0) "más" else "menos"} que con tu rendimiento anterior."
    }
    ProjectionCard(StatisticInfo.UsualRange, R.drawable.ic_route, "≈ ${number(range.distanceKm, 1)} km",
        "Si mantienes tu consumo reciente, una carga de ${number(range.liters, 1)} L podría rendirte aproximadamente ${number(range.distanceKm, 1)} km$comparison",
        basis = "Referencia: ${range.intervalCount} intervalos completos hasta ${forecastDate(range.through)}. Litros habituales: mediana de tus tres últimas cargas completas.")
}

@Composable
private fun PriceTrendCard(price: FuelPriceTrend?, modifier: Modifier) {
    if (price == null) {
        ProjectionCard(StatisticInfo.PriceTrend, R.drawable.ic_category_sell, "Faltan precios comparables",
            "Necesitas seis cargas incluidas del mismo combustible identificado.", modifier = modifier)
        return
    }
    val change = rounded(price.changePercent)
    ProjectionCard(StatisticInfo.PriceTrend, R.drawable.ic_category_sell,
        if (change == 0.0) "Precio similar" else "${if (change > 0) "+" else "−"}${number(abs(change), 1)} %",
        "${FuelType.fromCode(price.fuelType).label}: de ${money(price.previousPrice)} a ${money(price.recentPrice)} por litro.",
        modifier = modifier, basis = "Tres cargas recientes frente a tres anteriores, ponderadas por litros. ${forecastDate(price.start)} — ${forecastDate(price.end)}.")
}

@Composable
private fun SavingsCard(savings: FuelSavings?) {
    if (savings == null) {
        ProjectionCard(StatisticInfo.Savings, R.drawable.ic_water_drop, "Falta una tendencia comparable",
            "Necesitas seis intervalos completos válidos para comparar el consumo reciente y anterior.")
        return
    }
    val liters = rounded(savings.litersPer500Km)
    val amount = money(abs(savings.moneyPer500Km))
    ProjectionCard(StatisticInfo.Savings, R.drawable.ic_water_drop,
        when { liters > 0 -> "≈ $amount por cada 500 km"; liters < 0 -> "Ya mejoras tu consumo"; else -> "Consumo similar" },
        when {
            liters > 0 -> "Si recuperas tu rendimiento anterior, podrías ahorrar unos ${number(liters, 1)} L por cada 500 km, equivalentes a $amount."
            liters < 0 -> "Si mantienes tu rendimiento reciente, usarías unos ${number(abs(liters), 1)} L menos por cada 500 km que antes, equivalentes a $amount."
            else -> "La diferencia para 500 km es menor a 0.1 L. Mantener tu rendimiento conservaría un consumo similar."
        }, basis = "Estimación al precio medio reciente de ${money(savings.pricePerLiter)} por litro; supone mantener ese precio.")
}

@Composable
private fun MonthlySpendingCard(spending: MonthlySpending?, hasRecords: Boolean, today: LocalDate) {
    val month = today.format(DateTimeFormatter.ofPattern("MMMM yyyy", MexicanLocale))
    if (spending == null) {
        ProjectionCard(StatisticInfo.MonthlySpending, R.drawable.ic_receipt,
            if (hasRecords) "Faltan días de observación" else "Sin registros de este mes",
            "Para estimar el cierre de $month necesitas movimientos del mes y al menos siete días de observación.")
        return
    }
    ProjectionCard(StatisticInfo.MonthlySpending, R.drawable.ic_receipt, "≈ ${money(spending.estimatedTotal)}",
        "Si mantienes el ritmo registrado, cerrarías $month con ${money(spending.estimatedFuel)} de combustible y ${money(spending.estimatedExpenses)} de otros gastos.",
        basis = "Acumulado: ${money(spending.recordedTotal)} · ${spending.observedDays} días de observación · ${spending.remainingDays} días restantes. Incluye los gastos puntuales al ritmo registrado.")
}
