package mx.fuelstatus

import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.flow.first
import mx.fuelstatus.data.*
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class DatabaseTest {
    private fun csv() = "carName,carType,carYear,dateISO8601,amount,price,meters,fullFillup\n" +
        "Mazda 3,Hatchback,2025,2026-01-01T12:00:00Z,40,25,1000,true\n" +
        "Mazda 3,Hatchback,2025,2026-01-10T12:00:00Z,40,25,1400,true\n"

    @Test fun importIsIdempotentAndReusesDefaultVehicle() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val database = Room.inMemoryDatabaseBuilder(context, FuelDatabase::class.java).build()
        try {
            val repository = FuelRepository(database)
            repository.initialize()
            val input = FuelCsv.parse(csv())
            assertEquals(ImportResult(2, 0), repository.import(input))
            assertEquals(ImportResult(0, 2), repository.import(input))
            assertEquals(1, repository.dao.allVehicles().size)
            assertEquals(2, repository.dao.allRefuels().size)
            val edited = repository.dao.allRefuels().first().copy(notes = "Nota editada", costCents = 110000)
            repository.dao.save(edited)
            assertEquals(ImportResult(0, 2), repository.import(input))
            assertEquals(edited, repository.dao.allRefuels().first { it.id == edited.id })
        } finally { database.close() }
    }

    @Test fun refuelsAndExpensesSurviveDatabaseReopen() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val name = "fuelstatus-persistence-test.db"
        context.deleteDatabase(name)
        var database = Room.databaseBuilder(context, FuelDatabase::class.java, name).build()
        try {
            val repository = FuelRepository(database)
            repository.initialize(); repository.import(FuelCsv.parse(csv()))
            val vehicle = repository.dao.allVehicles().single()
            val expense = Expense(vehicleId = vehicle.id, timestamp = 1000, category = "Servicio", costCents = 50000)
            repository.dao.save(expense)
            database.close()
            database = Room.databaseBuilder(context, FuelDatabase::class.java, name).build()
            assertEquals(2, database.dao().allRefuels().size)
            assertEquals(1, database.dao().allVehicles().size)
            assertEquals(expense, database.dao().expenses().first().single())
            database.dao().delete(expense)
            database.dao().delete(database.dao().allRefuels().first())
            assertEquals(1, database.dao().allRefuels().size)
        } finally { database.close(); context.deleteDatabase(name) }
    }
    @Test fun migrationFromVersionOnePreservesHistoryAndInitializesRevision() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val name = "fuelstatus-migration-test.db"
        context.deleteDatabase(name)
        val schemaText = InstrumentationRegistry.getInstrumentation().context.assets
            .open("mx.fuelstatus.data.FuelDatabase/1.json").bufferedReader().use { it.readText() }
        val schema = com.google.gson.JsonParser.parseString(schemaText).asJsonObject.getAsJsonObject("database")
        context.openOrCreateDatabase(name, 0, null).use { old ->
            schema.getAsJsonArray("entities").forEach { entity ->
                val item = entity.asJsonObject
                val table = item.get("tableName").asString
                old.execSQL(item.get("createSql").asString.replace("\${TABLE_NAME}", table))
                item.getAsJsonArray("indices")?.forEach { index ->
                    old.execSQL(index.asJsonObject.get("createSql").asString.replace("\${TABLE_NAME}", table))
                }
            }
            schema.getAsJsonArray("setupQueries").forEach { old.execSQL(it.asString) }
            old.execSQL("INSERT INTO vehicles VALUES ('car', 'Mazda 3', 'Hatchback', 2025)")
            old.execSQL("INSERT INTO refuels VALUES ('fill', 'car', 1000, 1000000, 40000, 100000, 1, 1, 0, 'Estación', 'Original')")
            old.execSQL("INSERT INTO expenses VALUES ('expense', 'car', 1000, 'Servicio', 50000, 'Original')")
            old.version = 1
        }
        val database = Room.databaseBuilder(context, FuelDatabase::class.java, name)
            .addMigrations(FuelDatabase.MIGRATION_1_2).build()
        try {
            assertEquals(1, database.dao().allVehicles().size)
            assertEquals("Original", database.dao().allRefuels().single().notes)
            assertEquals(50000L, database.dao().allExpenses().single().costCents)
            assertEquals(1L, database.dao().revision())
            val repository = FuelRepository(database)
            repository.save(database.dao().allExpenses().single().copy(costCents = 60000))
            assertEquals(2L, database.dao().revision())
        } finally { database.close(); context.deleteDatabase(name) }
    }

    @Test fun restoreReplacesAllTablesAndFailedSaveDoesNotAdvanceRevision() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val database = Room.inMemoryDatabaseBuilder(context, FuelDatabase::class.java).build()
        val appearance = mx.fuelstatus.backup.AppearanceStore(context, "test-backup-appearance")
        try {
            val repository = FuelRepository(database); repository.initialize()
            val source = mx.fuelstatus.backup.RoomBackupSource(repository, appearance)
            val before = source.snapshot("test-device", 1000)
            try {
                repository.save(Expense(vehicleId = "missing", timestamp = 1000, category = "Servicio", costCents = 50000))
                fail("Expected foreign key failure")
            } catch (_: android.database.sqlite.SQLiteConstraintException) { }
            assertEquals(before.revision, source.revision())
            val vehicle = Vehicle("restored", "Otro auto", "Sedán", 2024)
            val fill = Refuel("restored-fill", vehicle.id, 1000, 1000000, 40000, 100000)
            val expense = Expense("restored-expense", vehicle.id, 1000, "Servicio", 55000)
            val restored = mx.fuelstatus.backup.BackupDocument(1000, "other-device", 100,
                listOf(vehicle), listOf(fill), listOf(expense), Preferences("dark", false, vehicle.id))
            source.restore(restored)
            val after = source.snapshot("test-device", 2000)
            assertEquals(listOf(vehicle), after.vehicles)
            assertEquals(listOf(fill), after.refuels)
            assertEquals(listOf(expense), after.expenses)
            assertEquals(restored.appearance, after.appearance)
            assertEquals(before.revision + 1, after.revision)
        } finally { database.close(); context.getSharedPreferences("test-backup-appearance", 0).edit().clear().commit() }
    }

}
