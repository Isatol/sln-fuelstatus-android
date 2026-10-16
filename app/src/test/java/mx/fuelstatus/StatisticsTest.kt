package mx.fuelstatus

import mx.fuelstatus.data.Refuel
import mx.fuelstatus.domain.statistics
import org.junit.Assert.*
import org.junit.Test

class StatisticsTest {
    private fun fill(km: Long, liters: Long, cost: Long = liters * 25, full: Boolean = true, include: Boolean = true, missed: Boolean = false) =
        Refuel(id = "$km", vehicleId = "car", timestamp = km, odometerMeters = km * 1000, volumeMl = liters * 1000,
            costCents = cost * 100, fullTank = full, included = include, missedPrevious = missed)

    @Test fun emptyAndFirstTankHaveNoConsumption() {
        assertNull(statistics(emptyList()).kmPerLiter)
        val result = statistics(listOf(fill(1000, 40)))
        assertNull(result.kmPerLiter); assertNull(result.costPerKm)
        assertEquals(40.0, result.liters, .0001); assertEquals(1000.0, result.fuelCost, .0001)
    }
    @Test fun firstTankIsExcludedFromConsumedLiters() {
        val result = statistics(listOf(fill(1000, 60), fill(1400, 40)))
        assertEquals(10.0, result.kmPerLiter!!, .0001)
        assertEquals(2.5, result.costPerKm!!, .0001)
        assertEquals(100.0, result.liters, .0001)
    }
    @Test fun partialTanksAccumulateUntilFull() {
        val result = statistics(listOf(fill(1000, 45), fill(1200, 15, full = false), fill(1500, 35)))
        assertEquals(1, result.periods.size)
        assertEquals(10.0, result.kmPerLiter!!, .0001)
    }
    @Test fun trailingPartialDoesNotDistortCompletedConsumption() {
        val result = statistics(listOf(fill(1000, 40), fill(1400, 40), fill(1500, 20, full = false)))
        assertEquals(10.0, result.kmPerLiter!!, .0001); assertEquals(100.0, result.liters, .0001)
    }
    @Test fun weightedAverageIsNotAverageOfRatios() {
        val result = statistics(listOf(fill(1000, 40), fill(1400, 40), fill(1500, 20)))
        assertEquals(500.0 / 60, result.kmPerLiter!!, .0001)
    }
    @Test fun missingFillBreaksInterval() {
        val result = statistics(listOf(fill(1000, 40), fill(1800, 40, missed = true), fill(2200, 40)))
        assertEquals(400.0, result.distanceKm, .0001)
    }
    @Test fun excludedFillBreaksIntervalAndDoesNotEnterTotals() {
        val result = statistics(listOf(fill(1000, 40), fill(1300, 20, include = false), fill(1700, 40), fill(2100, 40)))
        assertEquals(3, result.count); assertEquals(400.0, result.distanceKm, .0001)
        assertEquals(120.0, result.liters, .0001)
    }
    @Test fun inputOrderDoesNotMatter() {
        val result = statistics(listOf(fill(1400, 40), fill(1000, 40)))
        assertEquals(10.0, result.kmPerLiter!!, .0001)
    }
    @Test(expected = IllegalArgumentException::class) fun vehiclesAreNeverMixed() {
        statistics(listOf(fill(1000, 40), fill(2000, 40).copy(vehicleId = "other")))
    }
    @Test fun sameOdometerDoesNotProduceInfinity() {
        assertNull(statistics(listOf(fill(1000, 40), fill(1000, 30))).kmPerLiter)
    }
}
