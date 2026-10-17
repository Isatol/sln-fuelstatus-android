package mx.fuelstatus

import mx.fuelstatus.data.Refuel
import mx.fuelstatus.domain.*
import org.junit.Assert.*
import org.junit.Test
import java.time.*

class RefuelDateRangeTest {
    private val today = LocalDate.of(2026, 10, 16)
    private val zone = ZoneId.of("America/Monterrey")
    private val custom = RefuelDateRange(today.minusYears(2), today.minusYears(1))
    private fun fill(id: String, time: Instant) = Refuel(id, "car", time.toEpochMilli(), 1000000, 40000, 100000)

    @Test fun presetsUseCalendarMonthsAndEndToday() {
        assertEquals(RefuelDateRange(LocalDate.of(2026, 7, 16), today), RefuelHistoryPeriod.ThreeMonths.range(today, custom))
        assertEquals(RefuelDateRange(LocalDate.of(2026, 4, 16), today), RefuelHistoryPeriod.SixMonths.range(today, custom))
        assertEquals(RefuelDateRange(LocalDate.of(2025, 10, 16), today), RefuelHistoryPeriod.OneYear.range(today, custom))
        assertEquals(custom, RefuelHistoryPeriod.Custom.range(today, custom))
    }

    @Test fun monthEndsAndLeapYearsAreClampedToValidDates() {
        val may = LocalDate.of(2024, 5, 31)
        assertEquals(LocalDate.of(2024, 2, 29), RefuelHistoryPeriod.ThreeMonths.range(may, custom).start)
        val leap = LocalDate.of(2024, 2, 29)
        assertEquals(LocalDate.of(2023, 2, 28), RefuelHistoryPeriod.OneYear.range(leap, custom).start)
    }

    @Test fun startAndEndAreInclusiveForEntireLocalDays() {
        val range = RefuelHistoryPeriod.ThreeMonths.range(today, custom)
        val start = range.start.atStartOfDay(zone).toInstant()
        val nextDay = range.endInclusive.plusDays(1).atStartOfDay(zone).toInstant()
        val rows = listOf(fill("before", start.minusMillis(1)), fill("start", start),
            fill("last-millisecond", nextDay.minusMillis(1)), fill("after", nextDay))
        assertEquals(listOf("start", "last-millisecond"), filterRefuelsByDate(rows, range, zone).map { it.id })
    }

    @Test fun customCanSelectOneDayAndPreservesInputOrder() {
        val range = RefuelDateRange(today, today)
        val noon = today.atTime(12, 0).atZone(zone).toInstant()
        val rows = listOf(fill("late", noon.plusSeconds(60)), fill("early", noon), fill("outside", noon.minusSeconds(86400)))
        assertEquals(listOf("late", "early"), filterRefuelsByDate(rows, range, zone).map { it.id })
    }

    @Test fun localDatesDoNotUseUtcDayBoundaries() {
        val atNight = fill("night", Instant.parse("2026-10-17T04:30:00Z"))
        val range = RefuelDateRange(today, today)
        assertEquals(listOf(atNight), filterRefuelsByDate(listOf(atNight), range, zone))
        assertTrue(filterRefuelsByDate(listOf(atNight), range, ZoneOffset.UTC).isEmpty())
    }

    @Test fun daylightSavingDaysIncludeTheLastHourWithoutLeakingIntoTomorrow() {
        val dstZone = ZoneId.of("America/New_York")
        for (day in listOf(LocalDate.of(2026, 3, 8), LocalDate.of(2026, 11, 1))) {
            val end = day.plusDays(1).atStartOfDay(dstZone).toInstant()
            val rows = listOf(fill("last", end.minusMillis(1)), fill("next", end))
            assertEquals(listOf("last"), filterRefuelsByDate(rows, RefuelDateRange(day, day), dstZone).map { it.id })
        }
    }

    @Test fun invertedRangesAreRejectedAndFilteringNeverMutatesRecords() {
        assertThrows(IllegalArgumentException::class.java) { RefuelDateRange(today, today.minusDays(1)) }
        val old = fill("old", today.minusMonths(5).atStartOfDay(zone).toInstant())
        val recent = fill("recent", today.atTime(12, 0).atZone(zone).toInstant()).copy(odometerMeters = 1400000)
        val rows = listOf(recent, old)
        val before = statistics(rows)
        assertEquals(listOf(recent), filterRefuelsByDate(rows, RefuelHistoryPeriod.ThreeMonths.range(today, custom), zone))
        assertEquals(before, statistics(rows))
        assertEquals(10.0, before.kmPerLiter!!, .0001)
        assertTrue(filterRefuelsByDate(emptyList(), custom, zone).isEmpty())
    }
}
