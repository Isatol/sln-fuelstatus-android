package mx.fuelstatus.domain

import mx.fuelstatus.data.Expense
import mx.fuelstatus.data.FuelType
import mx.fuelstatus.data.Refuel
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.ChronoUnit
import kotlin.math.ceil

data class NextFullRefuel(val earliest: LocalDate, val typical: LocalDate, val latest: LocalDate,
    val intervalCount: Int, val partialSinceLastFull: Boolean)

data class UsualRefuelRange(val liters: Double, val distanceKm: Double, val previousDistanceKm: Double?,
    val intervalCount: Int, val through: LocalDate)

data class FuelPriceTrend(val fuelType: String, val previousPrice: Double, val recentPrice: Double,
    val start: LocalDate, val end: LocalDate) {
    val changePercent get() = (recentPrice / previousPrice - 1) * 100
}

/** Positive means an opportunity to save; negative means the recent consumption already saves fuel. */
data class FuelSavings(val litersPer500Km: Double, val pricePerLiter: Double) {
    val moneyPer500Km get() = litersPer500Km * pricePerLiter
}

data class MonthlySpending(val month: LocalDate, val observedDays: Long, val remainingDays: Long,
    val recordedFuel: Double, val recordedExpenses: Double) {
    val estimatedFuel get() = recordedFuel * (1 + remainingDays.toDouble() / observedDays)
    val estimatedExpenses get() = recordedExpenses * (1 + remainingDays.toDouble() / observedDays)
    val recordedTotal get() = recordedFuel + recordedExpenses
    val estimatedTotal get() = estimatedFuel + estimatedExpenses
}

data class StatisticsProjections(val nextFullRefuel: NextFullRefuel?, val usualRange: UsualRefuelRange?,
    val costPer100Km: Double?, val recentIntervalCount: Int, val completeIntervalCount: Int, val recentThrough: LocalDate?,
    val priceTrend: FuelPriceTrend?, val savings: FuelSavings?, val monthlySpending: MonthlySpending?,
    val hasCurrentMonthRecords: Boolean)

/** Conditional estimates from recorded habits; no tank capacity or remaining fuel is inferred. */
fun projections(refuels: List<Refuel>, expenses: List<Expense>, today: LocalDate,
    zone: ZoneId = ZoneId.systemDefault()): StatisticsProjections {
    require((refuels.map { it.vehicleId } + expenses.map { it.vehicleId }).distinct().size <= 1) {
        "Calcula cada vehículo por separado."
    }
    fun day(timestamp: Long) = Instant.ofEpochMilli(timestamp).atZone(zone).toLocalDate()
    // Future-dated rows are never observations for a forecast.
    val observed = refuels.filter { !day(it.timestamp).isAfter(today) }
    val observedExpenses = expenses.filter { !day(it.timestamp).isAfter(today) }
    val stats = statistics(observed, zone)
    val complete = stats.comparablePeriods.takeLast(6)
    val recent = complete.takeLast(3)
    val recentKm = recent.sumOf { it.distanceKm }
    val recentLiters = recent.sumOf { it.liters }
    val recentCost = recent.sumOf { it.cost }
    val through = recent.lastOrNull()?.let { day(it.endTimestamp) }
    val byId = observed.associateBy { it.id }

    val next = if (complete.size < 3) null else {
        val days = complete.map { ChronoUnit.DAYS.between(day(it.startTimestamp), day(it.endTimestamp)) }
        val typicalDays = median(days.map { it.toDouble() })
        if (typicalDays <= 0) null else {
            val last = complete.last()
            val anchor = day(last.endTimestamp)
            NextFullRefuel(anchor.plusDays(days.min()), anchor.plusDays(ceil(typicalDays).toLong()),
                anchor.plusDays(days.max()), complete.size,
                observed.any { it.included && !it.fullTank && it.timestamp >= last.endTimestamp &&
                    it.odometerMeters > byId.getValue(last.endId).odometerMeters })
        }
    }
    val usual = if (recent.size < 3) null else {
        // Actual full-refuel purchase volumes, rather than all fuel accumulated between full tanks.
        val usualLiters = median(recent.map { byId.getValue(it.endId).liters })
        UsualRefuelRange(usualLiters, usualLiters * recentKm / recentLiters,
            stats.trend?.previous?.kmPerLiter?.times(usualLiters), recent.size, requireNotNull(through))
    }

    val purchases = observed.filter { it.included && it.volumeMl > 0 && it.costCents > 0 }
        .sortedWith(compareBy<Refuel> { it.timestamp }.thenBy { it.odometerMeters }.thenBy { it.id })
    val latestFuel = purchases.lastOrNull()?.fuelType
    val sameFuel = purchases.filter { it.fuelType == latestFuel }.takeLast(6)
    val price = if (sameFuel.size < 6 || FuelType.fromCode(latestFuel.orEmpty()) == FuelType.Unspecified) null else {
        fun weighted(rows: List<Refuel>) = rows.sumOf { it.cost } / rows.sumOf { it.liters }
        FuelPriceTrend(requireNotNull(latestFuel), weighted(sameFuel.take(3)), weighted(sameFuel.takeLast(3)),
            day(sameFuel.first().timestamp), day(sameFuel.last().timestamp))
    }
    val savings = stats.trend?.let {
        FuelSavings((it.recent.litersPer100Km - it.previous.litersPer100Km) * 5, recentCost / recentLiters)
    }

    val month = today.withDayOfMonth(1)
    val included = observed.filter { it.included }
    val monthFuel = included.filter { !day(it.timestamp).isBefore(month) }
    val monthExpenses = observedExpenses.filter { !day(it.timestamp).isBefore(month) }
    val hasRecords = monthFuel.isNotEmpty() || monthExpenses.isNotEmpty()
    val firstObservation = (included.map { day(it.timestamp) } + observedExpenses.map { day(it.timestamp) }).minOrNull()
    val observedDays = firstObservation?.let { ChronoUnit.DAYS.between(maxOf(month, it), today) + 1 } ?: 0
    val monthly = if (!hasRecords || observedDays < 7) null else MonthlySpending(month, observedDays,
        (today.lengthOfMonth() - today.dayOfMonth).toLong(), monthFuel.sumOf { it.cost }, monthExpenses.sumOf { it.costCents } / 100.0)

    return StatisticsProjections(next, usual, if (recent.isEmpty()) null else recentCost / recentKm * 100,
        recent.size, complete.size, through, price, savings, monthly, hasRecords)
}

private fun median(values: List<Double>): Double {
    val sorted = values.sorted()
    val middle = sorted.size / 2
    return if (sorted.size % 2 == 1) sorted[middle] else (sorted[middle - 1] + sorted[middle]) / 2
}
