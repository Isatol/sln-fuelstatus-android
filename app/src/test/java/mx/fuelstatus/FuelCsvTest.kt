package mx.fuelstatus

import mx.fuelstatus.data.*
import org.junit.Assert.*
import org.junit.Assume.assumeTrue
import org.junit.Test
import java.io.File

class FuelCsvTest {
    private val header = "carName,carType,carYear,carIsEV,dateISO8601,amount,price,meters,fullFillup,showInStatistic,comment,gasStation"
    private val row = "Auto,Hatchback,2025,false,2026-01-01T01:00:00Z,30.5,24.12345,1000.5,true,true,,Estación"
    @Test fun fuelPriceIsPerLiterAndMetersColumnContainsKilometers() {
        val result = FuelCsv.parse("$header\n$row").single()
        assertEquals(30500L, result.refuel.volumeMl)
        assertEquals(73577L, result.refuel.costCents)
        assertEquals(1000500L, result.refuel.odometerMeters)
        assertEquals(2025, result.vehicle.year)
    }
    @Test fun bomAndWindowsLineEndingsWork() { assertEquals(1, FuelCsv.parse("\uFEFF$header\r\n$row\r\n").size) }
    @Test fun quotedCommasEscapedQuotesAndNewlinesWork() {
        val source = row.replace(",,Estación", ",\"Línea 1, \"\"nota\"\"\nLínea 2\",Estación")
        assertEquals("Línea 1, \"nota\"\nLínea 2", FuelCsv.parse("$header\n$source").single().refuel.notes)
    }
    @Test fun semicolonAndDecimalCommaWork() {
        val source = row.replace(',', ';').replace("30.5", "30,5")
        assertEquals(30500L, FuelCsv.parse("${header.replace(',', ';')}\n$source").single().refuel.volumeMl)
    }
    @Test(expected = IllegalArgumentException::class) fun malformedFileFailsBeforeImport() { FuelCsv.parse("$header\n$row\nmissing,columns") }
    @Test(expected = IllegalArgumentException::class) fun invalidBooleanFails() { FuelCsv.parse("$header\n${row.replace("true,true", "maybe,true")}") }
    @Test(expected = IllegalArgumentException::class) fun negativeLitersFail() { FuelCsv.parse("$header\n${row.replace("30.5", "-30.5")}") }
    @Test(expected = IllegalArgumentException::class) fun unclosedQuoteFails() { FuelCsv.parse("$header\n\"$row") }
    @Test(expected = IllegalArgumentException::class) fun wrongFormatFails() { FuelCsv.parse("date,volume\n2026-01-01,40") }
    @Test fun exportReimportPreservesMoneyPrecisionAndFlags() {
        val parsed = FuelCsv.parse("$header\n$row").single()
        val refuel = parsed.refuel.copy(costCents = 123456, missedPrevious = true, included = false, notes = "coma, \"comilla\"\nsalto")
        val copy = FuelCsv.parse(FuelCsv.export(listOf(parsed.vehicle), listOf(refuel))).single().refuel
        assertEquals(refuel.costCents, copy.costCents); assertEquals(refuel.volumeMl, copy.volumeMl)
        assertEquals(refuel.timestamp, copy.timestamp); assertEquals(refuel.notes, copy.notes)
        assertEquals(refuel.missedPrevious, copy.missedPrevious); assertEquals(refuel.included, copy.included)
    }
    @Test fun optionalLocalFuelExport() {
        val path = System.getProperty("fuel.csv").orEmpty()
        assumeTrue("Provide -PfuelCsv=/path/to/export.csv to verify the private export", path.isNotBlank())
        val rows = FuelCsv.parse(File(path).readText())
        assertTrue(rows.isNotEmpty())
        assertEquals(rows.size, rows.map { it.refuel.id }.distinct().size)
        val vehicles = rows.map { it.vehicle }.distinctBy { it.id }
        val exported = FuelCsv.export(vehicles, rows.map { it.refuel })
        val reimported = FuelCsv.parse(exported)
        assertEquals(rows.sortedBy { it.refuel.id }, reimported.sortedBy { it.refuel.id })
    }
}
