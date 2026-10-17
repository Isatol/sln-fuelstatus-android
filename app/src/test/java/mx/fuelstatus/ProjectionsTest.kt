package mx.fuelstatus

import mx.fuelstatus.data.*
import mx.fuelstatus.domain.*
import org.junit.Assert.*
import org.junit.Test
import java.time.*

class ProjectionsTest {
    private val zone = ZoneId.of("America/Monterrey")
    private val today = LocalDate.of(2026, 10, 17)
    private fun timestamp(day: LocalDate, targetZone: ZoneId = zone) = day.atTime(12, 0).atZone(targetZone).toInstant().toEpochMilli()
    private fun rows(recentLiters: Double = 44.0) = (0..6).map { i ->
        val liters = if (i > 3) recentLiters else 40.0
        val price = if (i > 3) 27.5 else 25.0
        Refuel("r$i", "car", timestamp(LocalDate.of(2026, 10, 4).plusDays(i * 2L)),
            1000000L + i * 400000L, (liters * 1000).toLong(), (liters * price * 100).toLong(), fuelType = FuelType.Magna.code)
    }
    private fun result(rows: List<Refuel> = rows(), expenses: List<Expense> = emptyList(), day: LocalDate = today) =
        projections(rows, expenses, day, zone)

    @Test fun estimatesUseActualRecentEfficiencyAndAConstantVolumeForTheComparison() {
        val p = result()
        val range = requireNotNull(p.usualRange)
        assertEquals(44.0, range.liters, .000001)
        assertEquals(400.0, range.distanceKm, .000001)
        assertEquals(440.0, range.previousDistanceKm!!, .000001)
        assertEquals(302.5, p.costPer100Km!!, .000001)
        assertEquals(5.0, p.savings!!.litersPer500Km, .000001)
        assertEquals(137.5, p.savings.moneyPer500Km, .000001)
        assertEquals(3, range.intervalCount)
    }

    @Test fun improvementProducesMoreRangeAndAnExistingSavingRatherThanANegativeOpportunity() {
        val p = result(rows(32.0))
        assertTrue(p.usualRange!!.distanceKm > p.usualRange.previousDistanceKm!!)
        assertEquals(-10.0, p.savings!!.litersPer500Km, .000001)
        assertEquals(-275.0, p.savings.moneyPer500Km, .000001)
        assertEquals(0.0, result(rows(40.0)).savings!!.litersPer500Km, .000001)
    }

    @Test fun nextFullRefuelUsesCalendarDayGapsAndTheLastCompleteAnchor() {
        val next = requireNotNull(result().nextFullRefuel)
        assertEquals(LocalDate.of(2026, 10, 18), next.earliest)
        assertEquals(next.earliest, next.typical); assertEquals(next.typical, next.latest)
        assertEquals(6, next.intervalCount); assertFalse(next.partialSinceLastFull)
        // An old forecast remains in the past instead of being moved to an invented future date.
        assertEquals(next, result(day = today.plusMonths(1)).nextFullRefuel)
    }

    @Test fun dateRangeUsesObservedMinimumMaximumAndMedianOfOnlyTheLatestSixIntervals() {
        val days = listOf(1, 2, 4, 7, 11, 16, 22, 29)
        val data = days.mapIndexed { i, d -> rows()[0].copy(id = "v$i", timestamp = timestamp(LocalDate.of(2026, 9, d)),
            odometerMeters = 1000000L + i * 400000L) }
        val next = requireNotNull(result(data).nextFullRefuel)
        assertEquals(LocalDate.of(2026, 10, 1), next.earliest) // latest six gaps: 2,3,4,5,6,7
        assertEquals(LocalDate.of(2026, 10, 4), next.typical) // ceil(median 4.5)
        assertEquals(LocalDate.of(2026, 10, 6), next.latest)
        assertEquals(6, next.intervalCount)
    }

    @Test fun fewerThanThreeIntervalsDoNotProduceDateOrRangeForecasts() {
        for (data in listOf(emptyList(), rows().take(1), rows().take(3))) {
            val p = result(data)
            assertNull(p.nextFullRefuel); assertNull(p.usualRange); assertNull(p.savings)
        }
        val p = result(rows().take(4))
        assertNotNull(p.nextFullRefuel); assertNotNull(p.usualRange)
        assertNull(p.usualRange!!.previousDistanceKm); assertNull(p.savings)
    }

    @Test fun exclusionsMissingFillsAndOdometerReversalsDoNotBridgeAnUnknownGap() {
        for (kind in 0..2) {
            val data = rows().mapIndexed { i, row -> if (i != 4) row else when (kind) {
                0 -> row.copy(included = false)
                1 -> row.copy(missedPrevious = true)
                else -> row.copy(odometerMeters = 900000)
            } }
            val p = result(data)
            assertNull(p.nextFullRefuel); assertNull(p.usualRange); assertNull(p.savings)
            assertTrue(p.completeIntervalCount < 3)
        }
    }

    @Test fun fullPurchaseMedianDoesNotMistakeAccumulatedPartialLitersForTheSizeOfOneLoad() {
        val base = rows()
        val data = base.mapIndexed { i, row -> if (i > 3) row.copy(volumeMl = 20000, costCents = 55000) else row } +
            (4..6).map { i -> base[i].copy(id = "p$i", timestamp = base[i].timestamp - 86400000,
                odometerMeters = base[i].odometerMeters - 200000, volumeMl = 24000, costCents = 66000, fullTank = false) }
        val range = requireNotNull(result(data.reversed()).usualRange)
        assertEquals(20.0, range.liters, .000001)
        assertEquals(20 * 1200.0 / 132, range.distanceKm, .000001)
    }

    @Test fun aPartialAfterTheAnchorIsFlaggedIncludingOneWithTheSameTimestamp() {
        for (time in listOf(rows().last().timestamp, timestamp(today))) {
            val data = rows() + rows().last().copy(id = "partial", timestamp = time, odometerMeters = 3500000,
                fullTank = false, volumeMl = 10000, costCents = 27500)
            val p = result(data)
            assertTrue(p.nextFullRefuel!!.partialSinceLastFull)
            assertEquals(result().usualRange, p.usualRange)
        }
    }

    @Test fun zeroDayCadenceDoesNotInventATomorrowForecast() {
        val p = result(rows().map { it.copy(timestamp = timestamp(today)) })
        assertNull(p.nextFullRefuel)
        assertNotNull(p.usualRange)
    }

    @Test fun forecastCadenceUsesLocalDatesAcrossDaylightSaving() {
        val ny = ZoneId.of("America/New_York")
        val data = rows().take(4).mapIndexed { i, row -> row.copy(timestamp = timestamp(LocalDate.of(2026, 3, 5 + i), ny)) }
        val next = requireNotNull(projections(data, emptyList(), LocalDate.of(2026, 3, 8), ny).nextFullRefuel)
        assertEquals(LocalDate.of(2026, 3, 9), next.typical)
    }

    @Test fun observationsAndFutureDatesRespectTheSelectedLocalCalendar() {
        val data = rows().take(4).mapIndexed { i, row -> row.copy(timestamp = Instant.parse("2026-10-${15 + i}T01:00:00Z").toEpochMilli()) }
        assertEquals(3, projections(data, emptyList(), today, zone).completeIntervalCount)
        assertEquals(2, projections(data, emptyList(), today, ZoneOffset.UTC).completeIntervalCount)
        val future = rows().last().copy(id = "future", timestamp = timestamp(today.plusDays(1)), odometerMeters = 9000000,
            volumeMl = 500000, costCents = 9000000)
        val expense = Expense("future-expense", "car", timestamp(today.plusDays(1)), "Servicio", 999999)
        assertEquals(result(), result(rows() + future, listOf(expense)))
    }

    @Test fun pricesAreWeightedByLitersAndOnlyCompareTheLatestFuelType() {
        val volumes = listOf(40, 10, 20, 70, 30, 30, 40)
        val prices = listOf(25, 20, 30, 40, 10, 20, 30)
        val data = rows().mapIndexed { i, row -> row.copy(volumeMl = volumes[i] * 1000L, costCents = volumes[i] * prices[i] * 100L) }
        val price = requireNotNull(result(data).priceTrend)
        assertEquals(36.0, price.previousPrice, .000001); assertEquals(21.0, price.recentPrice, .000001)
        assertTrue(price.changePercent < 0)
        for (fuel in listOf(FuelType.Premium.code, FuelType.Unspecified.code)) {
            assertNull(result(data.dropLast(1) + data.last().copy(fuelType = fuel)).priceTrend)
        }
        assertNull(result(data.map { it.copy(fuelType = FuelType.Unspecified.code) }).priceTrend)
        assertNull(result(data.take(5)).priceTrend)
    }

    @Test fun monthlyProjectionAddsObservedPaceOnlyForRemainingDays() {
        val expense = Expense("expense", "car", timestamp(today.minusDays(3)), "Servicio", 30000)
        val monthly = requireNotNull(result(expenses = listOf(expense)).monthlySpending)
        assertEquals(14L, monthly.observedDays); assertEquals(14L, monthly.remainingDays)
        assertEquals(7630.0, monthly.recordedFuel, .000001)
        assertEquals(300.0, monthly.recordedExpenses, .000001)
        assertEquals(15860.0, monthly.estimatedTotal, .000001)
        val end = requireNotNull(result(expenses = listOf(expense), day = today.withDayOfMonth(31)).monthlySpending)
        assertEquals(end.recordedTotal, end.estimatedTotal, .000001)
    }

    @Test fun monthlyProjectionNeedsCurrentRecordsAndSevenObservedDays() {
        assertNull(result(rows().take(1).map { it.copy(timestamp = timestamp(today)) }).monthlySpending)
        assertFalse(result(day = today.plusMonths(1)).hasCurrentMonthRecords)
        assertNull(result(day = today.plusMonths(1)).monthlySpending)
        assertNull(result(emptyList()).monthlySpending)
        val first = rows().first().copy(timestamp = timestamp(today.minusDays(6)))
        assertNotNull(result(listOf(first)).monthlySpending)
    }

    @Test fun monthlyProjectionFiltersExcludedFuelAndHandlesLeapMonthsAndExpensesOnly() {
        val leap = LocalDate.of(2028, 2, 14)
        val expenses = listOf(Expense("e", "car", timestamp(leap.withDayOfMonth(1)), "Servicio", 14000))
        val excluded = rows().first().copy(timestamp = timestamp(leap), included = false, costCents = 9999999)
        val p = projections(listOf(excluded), expenses, leap, zone)
        val monthly = requireNotNull(p.monthlySpending)
        assertEquals(0.0, monthly.estimatedFuel, .000001)
        assertEquals(290.0, monthly.estimatedExpenses, .000001)
        assertEquals(15L, monthly.remainingDays)
    }

    @Test(expected = IllegalArgumentException::class) fun projectionsNeverCombineVehicles() {
        result(expenses = listOf(Expense("other", "other-car", timestamp(today), "Servicio", 10000)))
    }
}
