package mx.fuelstatus

import mx.fuelstatus.data.Refuel
import mx.fuelstatus.domain.*
import org.junit.Assert.*
import org.junit.Test
import java.time.*

class RefuelInsightsTest {
    private val zone = ZoneId.of("America/Monterrey")
    private fun fill(id: Int, km: Long, liters: Double = 40.0, day: Int = id, full: Boolean = true) =
        Refuel(id.toString(), "car", LocalDate.of(2026, 9, 1).plusDays(day.toLong()).atTime(12, 0).atZone(zone).toInstant().toEpochMilli(),
            km * 1000, (liters * 1000).toLong(), (liters * 2500).toLong(), fullTank = full)
    private fun sequence(recentLiters: Double = 40.0) = (0..6).map { i -> fill(i, 1000L + i * 400, if (i > 3) recentLiters else 40.0) }
    private fun stats(rows: List<Refuel>) = statistics(rows, zone)

    @Test fun emptyAndSingleFillExplainMissingDataWithoutInventingAverages() {
        for (rows in listOf(emptyList(), listOf(fill(0, 1000)))) {
            val result = stats(rows)
            assertNull(result.averageDistanceBetweenRefuels); assertNull(result.averageDaysBetweenRefuels)
            assertNull(result.minimumDaysBetweenRefuels); assertNull(result.maximumDaysBetweenRefuels); assertNull(result.trend)
        }
    }

    @Test fun distanceUsesIntervalsRatherThanTheNumberOfRefuelsAndIncludesPartials() {
        val rows = listOf(fill(0, 1000), fill(1, 1100, full = false), fill(2, 1400), fill(3, 1550), fill(4, 1800, full = false))
        val result = stats(rows.reversed())
        assertEquals(4, result.refuelIntervals.size)
        assertEquals(200.0, result.averageDistanceBetweenRefuels!!, .000001)
    }

    @Test fun frequencyUsesCalendarDaysAndAllowsTwoFillsOnTheSameDay() {
        val result = stats(listOf(fill(0, 1000, day = 0), fill(1, 1200, day = 0), fill(2, 1400, day = 4), fill(3, 1800, day = 9)))
        assertEquals(3.0, result.averageDaysBetweenRefuels!!, .000001)
        assertEquals(0L, result.minimumDaysBetweenRefuels); assertEquals(5L, result.maximumDaysBetweenRefuels)
    }

    @Test fun daylightSavingAndTimeOfDayDoNotTurnOneCalendarDayIntoAFraction() {
        val dstZone = ZoneId.of("America/New_York")
        val a = fill(0, 1000).copy(timestamp = ZonedDateTime.of(2026, 3, 7, 23, 0, 0, 0, dstZone).toInstant().toEpochMilli())
        val b = fill(1, 1400).copy(timestamp = ZonedDateTime.of(2026, 3, 8, 23, 0, 0, 0, dstZone).toInstant().toEpochMilli())
        assertEquals(1.0, statistics(listOf(a, b), dstZone).averageDaysBetweenRefuels!!, .000001)
        assertEquals(23 * 60 * 60 * 1000L, b.timestamp - a.timestamp)
    }

    @Test fun missingAndExcludedFillsBreakGapsInsteadOfBeingFilteredOut() {
        val rows = (0..6).map { fill(it, 1000L + it * 100) }.toMutableList()
        rows[2] = rows[2].copy(included = false)
        rows[4] = rows[4].copy(missedPrevious = true)
        val result = stats(rows)
        assertEquals(3, result.refuelIntervals.size) // 0→1, 4→5, 5→6.
        assertEquals(100.0, result.averageDistanceBetweenRefuels!!, .000001)
    }

    @Test fun duplicateOrDecreasingOdometersDoNotProduceNegativeDistanceOrBridgeTheInvalidPair() {
        val result = stats(listOf(fill(0, 1000), fill(1, 1000), fill(2, 900), fill(3, 1200)))
        assertEquals(1, result.refuelIntervals.size)
        assertEquals(300.0, result.averageDistanceBetweenRefuels!!, .000001)
    }

    @Test fun trendNeedsSixCompleteIntervalsNotSixRowsOrManyPartialFills() {
        assertNull(stats(sequence().take(6)).trend)
        assertNotNull(stats(sequence()).trend)
        val partials = (0..10).map { fill(it, 1000L + it * 50, full = it == 0 || it == 10) }
        assertNull(stats(partials).trend)
    }

    @Test fun trendUsesBothAnchorDatesAndReportsIncreasingConsumptionWithTheCorrectSign() {
        val rows = sequence(43.2)
        val trend = requireNotNull(stats(rows.reversed()).trend)
        assertEquals(8.0, trend.changePercent, .000001)
        assertEquals(ConsumptionTrendStatus.Increasing, trend.status)
        assertEquals(rows[0].timestamp, trend.previous.startTimestamp); assertEquals(rows[3].timestamp, trend.previous.endTimestamp)
        assertEquals(rows[3].timestamp, trend.recent.startTimestamp); assertEquals(rows[6].timestamp, trend.recent.endTimestamp)
        assertEquals(10.0, trend.previous.litersPer100Km, .000001)
        assertEquals(10.8, trend.recent.litersPer100Km, .000001)
    }

    @Test fun trendUsesTotalsWeightedByDistanceRatherThanAnAverageOfIndividualRatios() {
        val rows = listOf(fill(0, 1000), fill(1, 1100, 20.0), fill(2, 1200, 10.0), fill(3, 2000, 70.0),
            fill(4, 2200, 20.0), fill(5, 2400, 20.0), fill(6, 3000, 60.0))
        val trend = requireNotNull(stats(rows).trend)
        assertEquals(10.0, trend.previous.litersPer100Km, .000001)
        assertEquals(10.0, trend.recent.litersPer100Km, .000001)
        assertEquals(ConsumptionTrendStatus.Stable, trend.status)
        assertEquals(10.0, trend.previous.kmPerLiter, .000001)
        assertEquals(10.0, trend.recent.kmPerLiter, .000001)
    }

    @Test fun fuelEquivalentUsesRecentDistanceEvenWhenFewerLitersWereConsumedOverall() {
        val trend = ConsumptionTrend(ConsumptionWindow(1, 2, 1500.0, 120.0), ConsumptionWindow(2, 3, 1200.0, 105.6))
        assertEquals(10.0, trend.changePercent, .000001)
        assertEquals(12.5, trend.previous.kmPerLiter, .000001)
        assertEquals(100.0 / 8.8, trend.recent.kmPerLiter, .000001)
        assertEquals(9.6, trend.equivalentLitersDifference, .000001)
        assertEquals(-300.0, trend.distanceDifferenceKm, .000001)
        assertEquals(-20.0, trend.distanceChangePercent, .000001)
        assertTrue(trend.recent.liters < trend.previous.liters)
    }

    @Test fun fuelSavingsCanCoexistWithMoreDistanceAndMoreTotalLiters() {
        val trend = ConsumptionTrend(ConsumptionWindow(1, 2, 1500.0, 120.0), ConsumptionWindow(2, 3, 1800.0, 129.6))
        assertEquals(-10.0, trend.changePercent, .000001)
        assertEquals(100.0 / 7.2, trend.recent.kmPerLiter, .000001)
        assertEquals(-14.4, trend.equivalentLitersDifference, .000001)
        assertEquals(300.0, trend.distanceDifferenceKm, .000001)
        assertEquals(20.0, trend.distanceChangePercent, .000001)
        assertTrue(trend.recent.liters > trend.previous.liters)
    }

    @Test fun equalEfficiencyProducesNoFuelDifferenceEvenWithDifferentDistances() {
        val trend = ConsumptionTrend(ConsumptionWindow(1, 2, 1500.0, 120.0), ConsumptionWindow(2, 3, 1200.0, 96.0))
        assertEquals(0.0, trend.equivalentLitersDifference, .000001)
        assertEquals(trend.previous.kmPerLiter, trend.recent.kmPerLiter, .000001)
        val equalDistance = trend.copy(recent = trend.recent.copy(distanceKm = 1500.0))
        assertEquals(0.0, equalDistance.distanceDifferenceKm, .000001)
        assertEquals(0.0, equalDistance.distanceChangePercent, .000001)
    }

    @Test fun fuelEquivalentDoesNotUseTheRoundedConsumptionPercentage() {
        val trend = ConsumptionTrend(ConsumptionWindow(1, 2, 1000.0, 80.0), ConsumptionWindow(2, 3, 1000.0, 83.999))
        assertEquals(5.0, trend.changePercent, .000001)
        assertEquals(3.999, trend.equivalentLitersDifference, .000001)
    }

    @Test fun trendAccumulatesPartialLitersAndIgnoresTrailingIncompleteConsumption() {
        val rows = sequence().toMutableList()
        rows[4] = rows[4].copy(volumeMl = 20000)
        rows += fill(40, 2400, 20.0, day = 3, full = false)
        rows += fill(70, 3500, 80.0, day = 7, full = false)
        val trend = requireNotNull(stats(rows).trend)
        assertEquals(0.0, trend.changePercent, .000001)
        assertEquals(sequence()[6].timestamp, trend.recent.endTimestamp)
    }

    @Test fun statusThresholdsUseTheDisplayedPrecisionAndAllFourLabelsAreReachable() {
        for ((percent, status) in listOf(-5.0 to ConsumptionTrendStatus.Improving, -4.9 to ConsumptionTrendStatus.Stable,
            0.0 to ConsumptionTrendStatus.Stable, 4.9 to ConsumptionTrendStatus.Stable, 5.0 to ConsumptionTrendStatus.Increasing,
            14.9 to ConsumptionTrendStatus.Increasing, 15.0 to ConsumptionTrendStatus.MarkedIncrease)) {
            val trend = ConsumptionTrend(ConsumptionWindow(1, 2, 100.0, 10.0), ConsumptionWindow(2, 3, 100.0, 10 * (1 + percent / 100)))
            assertEquals(percent, trend.changePercent, .000001); assertEquals(status, trend.status)
        }
        val rounded = ConsumptionTrend(ConsumptionWindow(1, 2, 100.0, 10.0), ConsumptionWindow(2, 3, 100.0, 10.496))
        assertEquals(5.0, rounded.changePercent, .000001); assertEquals(ConsumptionTrendStatus.Increasing, rounded.status)
    }

    @Test fun onlyTheLatestSixCompletePeriodsAreComparedAndTheInitialTankIsNotConsumed() {
        val rows = (0..8).map { fill(it, 1000L + it * 400, if (it <= 2) 90.0 else 40.0) }
        val trend = requireNotNull(stats(rows).trend)
        assertEquals(rows[2].timestamp, trend.previous.startTimestamp)
        assertEquals(0.0, trend.changePercent, .000001)
    }

    @Test fun trendRestartsAfterMissingOrExcludedFillsAndBecomesAvailableAfterSixNewPeriods() {
        for (excluded in listOf(false, true)) {
            val rows = (0..13).map { fill(it, 1000L + it * 400) }.toMutableList()
            rows[7] = rows[7].copy(included = !excluded, missedPrevious = !excluded)
            assertNull(stats(rows.take(9)).trend)
            if (excluded) rows += fill(14, 6600)
            assertNotNull(stats(rows).trend)
        }
    }

    @Test fun aPartialWithAnInconsistentDateCannotSneakIntoTheComparedPeriods() {
        val rows = sequence().toMutableList()
        rows += fill(99, 1700, 10.0, day = 0, full = false)
        assertNull(stats(rows).trend)
    }

    @Test(expected = IllegalArgumentException::class) fun vehicleHistoriesStayIndependent() {
        stats(listOf(fill(0, 1000), fill(1, 1400).copy(vehicleId = "other")))
    }
}
