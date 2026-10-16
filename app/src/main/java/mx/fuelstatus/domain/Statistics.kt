package mx.fuelstatus.domain

import mx.fuelstatus.data.Refuel

data class ConsumptionPeriod(val endId: String, val distanceKm: Double, val liters: Double, val cost: Double) {
    val kmPerLiter get() = distanceKm / liters
}
data class FuelStatistics(
    val count: Int, val liters: Double, val fuelCost: Double,
    val periods: List<ConsumptionPeriod>,
) {
    val distanceKm get() = periods.sumOf { it.distanceKm }
    val kmPerLiter: Double? get() = periods.sumOf { it.liters }.takeIf { it > 0 }?.let { distanceKm / it }
    val costPerKm: Double? get() = distanceKm.takeIf { it > 0 }?.let { km -> periods.sumOf { it.cost } / km }
    val averagePrice: Double? get() = liters.takeIf { it > 0 }?.let { fuelCost / it }
}

/** Full-to-full: the first full tank sets the baseline; subsequent fuel is consumed over the interval. */
fun statistics(refuels: List<Refuel>): FuelStatistics {
    require(refuels.map { it.vehicleId }.distinct().size <= 1) { "Calcula cada vehículo por separado." }
    val ordered = refuels.sortedWith(compareBy<Refuel> { it.odometerMeters }.thenBy { it.timestamp }.thenBy { it.id })
    val periods = mutableListOf<ConsumptionPeriod>()
    var anchor: Refuel? = null
    var liters = 0.0
    var cost = 0.0
    for (refuel in ordered) {
        if (!refuel.included || refuel.missedPrevious) {
            anchor = if (refuel.included && refuel.fullTank) refuel else null
            liters = 0.0; cost = 0.0
            continue
        }
        val start = anchor
        if (start == null) {
            if (refuel.fullTank) anchor = refuel
            continue
        }
        if (refuel.odometerMeters <= start.odometerMeters) {
            anchor = if (refuel.fullTank) refuel else null
            liters = 0.0; cost = 0.0
            continue
        }
        liters += refuel.liters
        cost += refuel.cost
        if (refuel.fullTank) {
            if (liters > 0) periods += ConsumptionPeriod(refuel.id, refuel.km - start.km, liters, cost)
            anchor = refuel; liters = 0.0; cost = 0.0
        }
    }
    val included = ordered.filter { it.included }
    return FuelStatistics(included.size, included.sumOf { it.liters }, included.sumOf { it.cost }, periods)
}
