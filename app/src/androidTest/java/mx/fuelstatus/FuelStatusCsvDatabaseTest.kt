package mx.fuelstatus

import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.runBlocking
import mx.fuelstatus.data.*
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class FuelStatusCsvDatabaseTest {
    private val context get() = InstrumentationRegistry.getInstrumentation().targetContext

    @Test fun nativeCsvImportsChosenVehiclesWithTheirOriginalFuelPaymentsPrecisionAndFlags() = runBlocking {
        val db = Room.inMemoryDatabaseBuilder(context, FuelDatabase::class.java).build()
        try {
            val repo = FuelRepository(db); repo.initialize()
            val vehicles = listOf(Vehicle("first", "Auto", "Sedán", 2024, "premium"),
                Vehicle("second", "Camioneta", "SUV", 2025, "diesel"))
            val cards = listOf(PaymentMethod("visa", "Visa personal", "visa"),
                PaymentMethod("mastercard", "Mastercard personal", "mastercard"), PaymentMethod("amex", "Amex personal", "amex"))
            val rows = (0..5).map { index -> Refuel("fill-$index", if (index < 3) "first" else "second",
                1767225600000L + index * 86400000L, 1000000L + index * 400000L, 30123, 123456L + index,
                fullTank = index % 2 == 0, included = index != 2, missedPrevious = index == 3,
                station = "Estación, centro", notes = "Nota \"original\"\nOtra línea", fuelType = listOf("magna", "premium", "diesel", "unspecified")[index % 4],
                paymentMethodId = listOf("visa", "mastercard", "amex", "cash", null, "visa")[index]) }
            val input = FuelCsv.parse(FuelCsv.export(vehicles, rows, listOf(PaymentMethod.Cash) + cards), CsvSource.FuelStatus)
            val existing = vehicles.first().copy(id = "existing", fuelType = "magna")
            val existingCard = cards.first().copy(id = "existing-visa", name = "VISA PERSONAL")
            repo.save(existing); repo.addPaymentMethod(existingCard)
            val firstSource = input.first { it.vehicle.name == "Auto" }.vehicle.id
            assertEquals(ImportResult(3, 0), repo.import(input, ImportPlan(firstSource, existing.id, source = CsvSource.FuelStatus)))
            assertEquals(1, repo.dao.allVehicles().size)
            val secondSource = input.first { it.vehicle.name == "Camioneta" }.vehicle.id
            assertEquals(ImportResult(3, 0), repo.import(input, ImportPlan(secondSource,
                newVehicleFuelType = "diesel", source = CsvSource.FuelStatus)))
            assertEquals(existing, repo.dao.allVehicles().single { it.id == existing.id })
            assertEquals("diesel", repo.dao.allVehicles().single { it.name == "Camioneta" }.fuelType)
            val imported = repo.dao.allRefuels().sortedBy { it.timestamp }
            val methods = repo.dao.allPaymentMethods().associateBy { it.id }
            rows.zip(imported).forEach { (original, actual) ->
                assertEquals(original.copy(id = actual.id, vehicleId = actual.vehicleId, paymentMethodId = actual.paymentMethodId), actual)
                val originalMethod = (listOf(PaymentMethod.Cash) + cards).firstOrNull { it.id == original.paymentMethodId }
                val actualMethod = methods[actual.paymentMethodId]
                assertEquals(originalMethod?.brand, actualMethod?.brand)
                assertTrue(originalMethod?.name.equals(actualMethod?.name, ignoreCase = true))
            }
            assertEquals(existingCard.id, imported.first().paymentMethodId)
            assertEquals(4, methods.size)
            val revision = repo.dao.revision()
            assertEquals(ImportResult(0, 3), repo.import(input, ImportPlan(firstSource, existing.id, source = CsvSource.FuelStatus)))
            assertEquals(revision, repo.dao.revision())
        } finally { db.close() }
    }

    @Test fun duplicateNativeRowsDoNotRecreateRemovedCardsOrOverwriteEditedRecords() = runBlocking {
        val db = Room.inMemoryDatabaseBuilder(context, FuelDatabase::class.java).build()
        try {
            val repo = FuelRepository(db); repo.initialize()
            val vehicle = Vehicle("car", "Auto", "Sedán", 2024)
            val card = PaymentMethod("card", "Personal", "amex")
            val row = Refuel("fill", vehicle.id, 1767225600000L, 1000000, 40000, 123456, fuelType = "premium", paymentMethodId = card.id)
            val input = FuelCsv.parse(FuelCsv.export(listOf(vehicle), listOf(row), listOf(PaymentMethod.Cash, card)), CsvSource.FuelStatus)
            repo.save(vehicle)
            val plan = ImportPlan(input.single().vehicle.id, vehicle.id, source = CsvSource.FuelStatus)
            repo.import(input, plan)
            val importedCard = repo.dao.allPaymentMethods().single { it.id != "cash" }
            repo.deletePaymentMethod(importedCard.id)
            val edited = repo.dao.allRefuels().single().copy(costCents = 99000, fuelType = "diesel", notes = "Editado")
            repo.save(edited)
            val revision = repo.dao.revision()
            assertEquals(ImportResult(0, 1), repo.import(input, plan))
            assertEquals(listOf(edited), repo.dao.allRefuels())
            assertEquals(listOf(PaymentMethod.Cash), repo.dao.allPaymentMethods())
            assertEquals(revision, repo.dao.revision())
        } finally { db.close() }
    }
}
