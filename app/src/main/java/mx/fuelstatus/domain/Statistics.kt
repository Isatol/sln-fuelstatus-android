package mx.fuelstatus.domain

import mx.fuelstatus.data.Refuel
import java.time.Instant
import java.time.ZoneId
import java.time.temporal.ChronoUnit
import kotlin.math.round

data class ConsumptionPeriod(val endId: String, val distanceKm: Double, val liters: Double, val cost: Double,
    val startId: String, val startTimestamp: Long, val endTimestamp: Long, val refuelIds: List<String>) {
    val kmPerLiter get() = distanceKm / liters
}
data class FuelStatistics(
    val count: Int, val liters: Double, val fuelCost: Double,
    val periods: List<ConsumptionPeriod>,
    val refuelIntervals: List<RefuelInterval>,
    val trend: ConsumptionTrend?,
) {
    val averageDistanceBetweenRefuels: Double? get() = refuelIntervals.takeIf { it.isNotEmpty() }?.map { it.distanceKm }?.average()
    val averageDaysBetweenRefuels: Double? get() = refuelIntervals.takeIf { it.isNotEmpty() }?.map { it.days.toDouble() }?.average()
    val minimumDaysBetweenRefuels: Long? get() = refuelIntervals.minOfOrNull { it.days }
    val maximumDaysBetweenRefuels: Long? get() = refuelIntervals.maxOfOrNull { it.days }
    val distanceKm get() = periods.sumOf { it.distanceKm }
    val kmPerLiter: Double? get() = periods.sumOf { it.liters }.takeIf { it > 0 }?.let { distanceKm / it }
    val costPerKm: Double? get() = distanceKm.takeIf { it > 0 }?.let { km -> periods.sumOf { it.cost } / km }
    val averagePrice: Double? get() = liters.takeIf { it > 0 }?.let { fuelCost / it }
}

/** Full-to-full: the first full tank sets the baseline; subsequent fuel is consumed over the interval. */
fun statistics(refuels: List<Refuel>, zone: ZoneId = ZoneId.systemDefault()): FuelStatistics {
    require(refuels.map { it.vehicleId }.distinct().size <= 1) { "Calcula cada vehículo por separado." }
    val ordered = refuels.sortedWith(compareBy<Refuel> { it.odometerMeters }.thenBy { it.timestamp }.thenBy { it.id })
    val periods = mutableListOf<ConsumptionPeriod>()
    var anchor: Refuel? = null
    var liters = 0.0
    var cost = 0.0
    val pendingIds = mutableListOf<String>()
    for (refuel in ordered) {
        if (!refuel.included || refuel.missedPrevious) {
            anchor = if (refuel.included && refuel.fullTank) refuel else null
            liters = 0.0; cost = 0.0; pendingIds.clear()
            continue
        }
        val start = anchor
        if (start == null) {
            if (refuel.fullTank) anchor = refuel
            continue
        }
        if (refuel.odometerMeters <= start.odometerMeters) {
            anchor = if (refuel.fullTank) refuel else null
            liters = 0.0; cost = 0.0; pendingIds.clear()
            continue
        }
        pendingIds.add(refuel.id)
        liters += refuel.liters
        cost += refuel.cost
        if (refuel.fullTank) {
            if (liters > 0) periods += ConsumptionPeriod(refuel.id, refuel.km - start.km, liters, cost, start.id, start.timestamp, refuel.timestamp, listOf(start.id) + pendingIds)
            anchor = refuel; liters = 0.0; cost = 0.0; pendingIds.clear()
        }
    }
    val included = ordered.filter { it.included }
    val chronological = refuels.sortedWith(compareBy<Refuel> { it.timestamp }.thenBy { it.odometerMeters }.thenBy { it.id })
    val intervals = mutableListOf<RefuelInterval>()
    val latestBlock = mutableSetOf<String>()
    chronological.firstOrNull()?.takeIf { it.included }?.let { latestBlock.add(it.id) }
    chronological.zipWithNext().forEach { (start, end) ->
        // Keep excluded/missing rows in the sequence: filtering them first would bridge an unknown gap.
        val valid = start.included && end.included && !end.missedPrevious && end.odometerMeters > start.odometerMeters
        if (valid) {
            val startDate = Instant.ofEpochMilli(start.timestamp).atZone(zone).toLocalDate()
            val endDate = Instant.ofEpochMilli(end.timestamp).atZone(zone).toLocalDate()
            intervals += RefuelInterval(end.km - start.km, ChronoUnit.DAYS.between(startDate, endDate))
            latestBlock.add(start.id); latestBlock.add(end.id)
        } else {
            latestBlock.clear()
            if (end.included) latestBlock.add(end.id)
        }
    }
    val comparable = periods.filter { it.refuelIds.all { id -> id in latestBlock } }
    val trend = if (comparable.size < 6) null else {
        val lastSix = comparable.takeLast(6)
        ConsumptionTrend(ConsumptionWindow.from(lastSix.take(3)), ConsumptionWindow.from(lastSix.takeLast(3)))
    }
    return FuelStatistics(included.size, included.sumOf { it.liters }, included.sumOf { it.cost }, periods, intervals, trend)
}

/** Calendar-day gaps between consecutive recorded visits, including partial refuels. */
data class RefuelInterval(val distanceKm: Double, val days: Long)

data class ConsumptionWindow(val startTimestamp: Long, val endTimestamp: Long, val distanceKm: Double, val liters: Double) {
    val litersPer100Km get() = liters / distanceKm * 100
    companion object {
        fun from(periods: List<ConsumptionPeriod>) = ConsumptionWindow(periods.first().startTimestamp,
            periods.last().endTimestamp, periods.sumOf { it.distanceKm }, periods.sumOf { it.liters })
    }
}

enum class ConsumptionTrendStatus { Improving, Stable, Increasing, MarkedIncrease }

data class ConsumptionTrend(val previous: ConsumptionWindow, val recent: ConsumptionWindow) {
    // Classify the displayed precision, so a rounded 5.0% cannot be labelled stable.
    val changePercent: Double get() {
        val rounded = round((recent.litersPer100Km / previous.litersPer100Km - 1) * 1000) / 10
        return if (rounded == 0.0) 0.0 else rounded
    }
    val status get() = when {
        changePercent <= -5 -> ConsumptionTrendStatus.Improving
        changePercent < 5 -> ConsumptionTrendStatus.Stable
        changePercent < 15 -> ConsumptionTrendStatus.Increasing
        else -> ConsumptionTrendStatus.MarkedIncrease
    }
}
