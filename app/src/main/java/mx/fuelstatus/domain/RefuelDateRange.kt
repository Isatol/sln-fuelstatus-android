package mx.fuelstatus.domain

import mx.fuelstatus.data.Refuel
import java.time.LocalDate
import java.time.ZoneId

enum class RefuelHistoryPeriod(val label: String, private val months: Long) {
    ThreeMonths("3 meses", 3), SixMonths("6 meses", 6), OneYear("1 año", 12), Custom("Personalizado", 0);

    fun range(today: LocalDate, custom: RefuelDateRange): RefuelDateRange =
        if (this == Custom) custom else RefuelDateRange(today.minusMonths(months), today)
}

data class RefuelDateRange(val start: LocalDate, val endInclusive: LocalDate) {
    init { require(!start.isAfter(endInclusive)) { "La fecha de inicio debe ser anterior o igual a la fecha de fin." } }
}

/** Calendar dates in the device's time zone; the end date includes the entire local day, even across DST. */
fun filterRefuelsByDate(refuels: List<Refuel>, range: RefuelDateRange, zone: ZoneId): List<Refuel> {
    val from = range.start.atStartOfDay(zone).toInstant().toEpochMilli()
    val until = range.endInclusive.plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli()
    return refuels.filter { it.timestamp >= from && it.timestamp < until }
}
