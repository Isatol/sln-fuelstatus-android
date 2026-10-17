package mx.fuelstatus

import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.google.gson.JsonParser
import kotlinx.coroutines.runBlocking
import mx.fuelstatus.backup.*
import mx.fuelstatus.data.*
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class PaymentDatabaseTest {
    private val context get() = InstrumentationRegistry.getInstrumentation().targetContext
    private fun rows() = FuelCsv.parse("carName,carType,carYear,dateISO8601,amount,price,meters,fullFillup\n" +
        "Auto,Sedán,2024,2026-01-01T12:00:00Z,40,25,1000,true\n" +
        "Auto,Sedán,2024,2026-01-10T12:00:00Z,40,25,1400,true\n")

    @Test fun versionThreeMigrationPreservesHistoryAndDoesNotGuessPayment() = runBlocking {
        val name = "fuelstatus-payment-migration.db"
        context.deleteDatabase(name)
        val schemaText = InstrumentationRegistry.getInstrumentation().context.assets
            .open("mx.fuelstatus.data.FuelDatabase/3.json").bufferedReader().use { it.readText() }
        val schema = JsonParser.parseString(schemaText).asJsonObject.getAsJsonObject("database")
        context.openOrCreateDatabase(name, 0, null).use { old ->
            schema.getAsJsonArray("entities").forEach { entity ->
                val item = entity.asJsonObject
                val table = item.get("tableName").asString
                old.execSQL(item.get("createSql").asString.replace("\${TABLE_NAME}", table))
                item.getAsJsonArray("indices")?.forEach { index -> old.execSQL(index.asJsonObject.get("createSql").asString.replace("\${TABLE_NAME}", table)) }
            }
            schema.getAsJsonArray("setupQueries").forEach { old.execSQL(it.asString) }
            old.execSQL("INSERT INTO vehicles VALUES ('car', 'Auto', 'Sedán', 2024, 'premium')")
            old.execSQL("INSERT INTO refuels VALUES ('fill', 'car', 1000, 1000000, 40000, 100000, 1, 1, 0, 'Estación', 'Original', 'premium')")
            old.execSQL("INSERT INTO backup_revision VALUES (1, 9)")
            old.version = 3
        }
        val db = Room.databaseBuilder(context, FuelDatabase::class.java, name).addMigrations(FuelDatabase.MIGRATION_3_4, FuelDatabase.MIGRATION_4_5, FuelDatabase.MIGRATION_5_6).build()
        try {
            assertEquals(listOf(PaymentMethod.Cash), db.dao().allPaymentMethods())
            val fill = db.dao().allRefuels().single()
            assertNull(fill.paymentMethodId)
            assertEquals("Original", fill.notes); assertEquals("premium", fill.fuelType); assertEquals(100000L, fill.costCents)
            assertEquals(11L, db.dao().revision())
            try { FuelRepository(db).save(fill.copy(paymentMethodId = "missing")); fail("Expected foreign key failure") }
            catch (_: android.database.sqlite.SQLiteConstraintException) { }
            assertEquals(11L, db.dao().revision())
        } finally { db.close(); context.deleteDatabase(name) }
    }

    @Test fun cardsAndAssignmentsSurviveReopenRestoreAndFullReset() = runBlocking {
        val name = "fuelstatus-payment-persistence.db"
        context.deleteDatabase(name)
        var db = Room.databaseBuilder(context, FuelDatabase::class.java, name).build()
        val appearance = AppearanceStore(context, "test-payment-appearance")
        try {
            var repo = FuelRepository(db); repo.initialize()
            val card = PaymentMethod("card", "Visa personal", "visa")
            repo.addPaymentMethod(card)
            repo.import(rows(), ImportPlan(rows().first().vehicle.id, paymentMethodId = card.id))
            val snapshot = RoomBackupSource(repo, appearance).snapshot("phone", 1000)
            val revision = snapshot.revision
            try { repo.addPaymentMethod(card.copy(id = "duplicate", name = " visa PERSONAL ")); fail("Duplicate card") }
            catch (_: IllegalArgumentException) { }
            assertEquals(revision, repo.dao.revision())
            db.close(); db = Room.databaseBuilder(context, FuelDatabase::class.java, name).build(); repo = FuelRepository(db)
            assertEquals(listOf(PaymentMethod.Cash, card), repo.dao.allPaymentMethods())
            assertTrue(repo.dao.allRefuels().all { it.paymentMethodId == card.id })
            val source = RoomBackupSource(repo, appearance)
            source.reset()
            assertEquals(listOf(PaymentMethod.Cash), repo.dao.allPaymentMethods())
            assertTrue(repo.dao.allRefuels().isEmpty())
            source.restore(BackupCodec.decode(BackupCodec.encode(snapshot)))
            assertEquals(snapshot.paymentMethods, repo.dao.allPaymentMethods())
            assertEquals(snapshot.refuels, repo.dao.allRefuels())
            source.reset(); repo.initialize()
            assertTrue(repo.dao.allVehicles().isEmpty())
            assertEquals(listOf(PaymentMethod.Cash), repo.dao.allPaymentMethods())
        } finally { db.close(); context.deleteDatabase(name); context.getSharedPreferences("test-payment-appearance", 0).edit().clear().commit() }
    }

    @Test fun importAssignsChosenMethodOnlyToNewRecordsAndCsvPreservesMethods() = runBlocking {
        val db = Room.inMemoryDatabaseBuilder(context, FuelDatabase::class.java).build()
        val second = Room.inMemoryDatabaseBuilder(context, FuelDatabase::class.java).build()
        try {
            val repo = FuelRepository(db); repo.initialize()
            val input = rows()
            val card = PaymentMethod("card", "Tarjeta gasolina", "mastercard")
            repo.addPaymentMethod(card)
            val plan = ImportPlan(input.first().vehicle.id, paymentMethodId = card.id)
            assertEquals(ImportResult(2, 0), repo.import(input, plan))
            val target = repo.dao.allVehicles().single()
            assertTrue(repo.dao.allRefuels().all { it.paymentMethodId == card.id })
            assertEquals(ImportResult(0, 2), repo.import(input, plan.copy(targetVehicleId = target.id, paymentMethodId = PaymentMethod.Cash.id)))
            assertTrue(repo.dao.allRefuels().all { it.paymentMethodId == card.id })
            val revision = repo.dao.revision()
            try { repo.import(input, plan.copy(targetVehicleId = target.id, paymentMethodId = "missing")); fail("Invalid payment method") }
            catch (_: IllegalArgumentException) { }
            assertEquals(revision, repo.dao.revision())
            val exported = FuelCsv.export(repo.dao.allVehicles(), repo.dao.allRefuels(), repo.dao.allPaymentMethods())
            val copy = FuelRepository(second); copy.initialize()
            assertEquals(ImportResult(2, 0), copy.import(FuelCsv.parse(exported)))
            assertEquals(ImportResult(0, 2), copy.import(FuelCsv.parse(exported)))
            val importedCard = copy.dao.allPaymentMethods().single { it.id != PaymentMethod.Cash.id }
            assertEquals(card.name, importedCard.name); assertEquals(card.brand, importedCard.brand)
            assertTrue(copy.dao.allRefuels().all { it.paymentMethodId == importedCard.id })
        } finally { db.close(); second.close() }
    }
}
