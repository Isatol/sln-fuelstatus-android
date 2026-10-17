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
            .addMigrations(FuelDatabase.MIGRATION_1_2, FuelDatabase.MIGRATION_2_3, FuelDatabase.MIGRATION_3_4, FuelDatabase.MIGRATION_4_5).build()
        try {
            assertEquals(1, database.dao().allVehicles().size)
            assertEquals("Original", database.dao().allRefuels().single().notes)
            assertEquals(50000L, database.dao().allExpenses().single().costCents)
            assertEquals(2L, database.dao().revision())
            val repository = FuelRepository(database)
            repository.save(database.dao().allExpenses().single().copy(costCents = 60000))
            assertEquals(3L, database.dao().revision())
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

    @Test fun versionTwoMigrationPreservesHistoryAndLeavesHistoricalFuelUnknown() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val name = "fuelstatus-v2-migration.db"
        context.deleteDatabase(name)
        val schemaText = InstrumentationRegistry.getInstrumentation().context.assets
            .open("mx.fuelstatus.data.FuelDatabase/2.json").bufferedReader().use { it.readText() }
        val schema = com.google.gson.JsonParser.parseString(schemaText).asJsonObject.getAsJsonObject("database")
        context.openOrCreateDatabase(name, 0, null).use { old ->
            schema.getAsJsonArray("entities").forEach { entity ->
                val item = entity.asJsonObject; val table = item.get("tableName").asString
                old.execSQL(item.get("createSql").asString.replace("\${TABLE_NAME}", table))
                item.getAsJsonArray("indices")?.forEach { index -> old.execSQL(index.asJsonObject.get("createSql").asString.replace("\${TABLE_NAME}", table)) }
            }
            schema.getAsJsonArray("setupQueries").forEach { old.execSQL(it.asString) }
            old.execSQL("INSERT INTO vehicles VALUES ('car', 'Mazda 3', 'Hatchback', 2025)")
            old.execSQL("INSERT INTO refuels VALUES ('fill', 'car', 1000, 1000000, 40000, 100000, 1, 1, 0, 'Estación', 'Original')")
            old.execSQL("INSERT INTO backup_revision VALUES (1, 9)")
            old.version = 2
        }
        val database = Room.databaseBuilder(context, FuelDatabase::class.java, name).addMigrations(FuelDatabase.MIGRATION_2_3, FuelDatabase.MIGRATION_3_4, FuelDatabase.MIGRATION_4_5).build()
        try {
            assertEquals(FuelType.Magna.code, database.dao().allVehicles().single().fuelType)
            assertEquals(FuelType.Unspecified.code, database.dao().allRefuels().single().fuelType)
            assertEquals(100000L, database.dao().allRefuels().single().costCents)
            assertEquals("Original", database.dao().allRefuels().single().notes)
            assertEquals(10L, database.dao().revision())
        } finally { database.close(); context.deleteDatabase(name) }
    }

    @Test fun importTargetsExistingVehicleInheritsFuelAndKeepsDuplicatesUntouched() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val db = Room.inMemoryDatabaseBuilder(context, FuelDatabase::class.java).build()
        try {
            val repo = FuelRepository(db); repo.initialize()
            val target = Vehicle("target", "Mi auto", "Sedán", 2024, FuelType.Premium.code)
            repo.save(target)
            val rows = FuelCsv.parse(csv())
            val plan = ImportPlan(rows.first().vehicle.id, target.id, true)
            assertEquals(ImportResult(2, 0), repo.import(rows, plan))
            assertEquals(listOf(target), repo.dao.allVehicles())
            assertTrue(repo.dao.allRefuels().all { it.vehicleId == target.id && it.fuelType == FuelType.Premium.code })
            val edited = repo.dao.allRefuels().first().copy(fuelType = FuelType.Magna.code, notes = "Edición manual")
            repo.save(edited)
            assertEquals(ImportResult(0, 2), repo.import(rows, plan))
            assertEquals(edited, repo.dao.allRefuels().first { it.id == edited.id })
            val second = target.copy(id = "second", name = "Otro auto", fuelType = FuelType.Diesel.code)
            repo.save(second)
            val mixed = rows.mapIndexed { index, row -> row.copy(refuel = row.refuel.copy(fuelType = if (index == 0) "magna" else "unspecified")) }
            assertEquals(ImportResult(2, 0), repo.import(mixed, plan.copy(targetVehicleId = second.id, useVehicleFuel = false)))
            assertEquals(setOf("magna", "unspecified"), repo.dao.allRefuels().filter { it.vehicleId == second.id }.map { it.fuelType }.toSet())
            assertEquals(4, repo.dao.allRefuels().size)
        } finally { db.close() }
    }

    @Test fun importRecognizesEditedRecordsImportedByPreviousVersions() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val db = Room.inMemoryDatabaseBuilder(context, FuelDatabase::class.java).build()
        try {
            val repo = FuelRepository(db); repo.initialize()
            val target = Vehicle("previous-vehicle", "Mi auto", "Sedán", 2024, "premium")
            repo.save(target)
            val rows = FuelCsv.parse(csv())
            val edited = rows.first().refuel.copy(vehicleId = target.id, costCents = 123456, notes = "Edición anterior", fuelType = "magna")
            repo.save(edited)
            val plan = ImportPlan(rows.first().vehicle.id, target.id)
            assertEquals(ImportResult(1, 1), repo.import(rows, plan))
            assertEquals(edited, repo.dao.allRefuels().first { it.id == edited.id })
            val other = target.copy(id = "other", name = "Otro auto")
            repo.save(other)
            assertEquals(ImportResult(2, 0), repo.import(rows, plan.copy(targetVehicleId = other.id)))
        } finally { db.close() }
    }

    @Test fun importCanCreateFoundVehicleAndOnlyImportsChosenSource() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val db = Room.inMemoryDatabaseBuilder(context, FuelDatabase::class.java).build()
        try {
            val repo = FuelRepository(db); repo.initialize()
            assertTrue(repo.dao.allVehicles().isEmpty())
            val original = FuelCsv.parse(csv())
            val other = original.first().let { it.copy(vehicle = it.vehicle.copy(id = "other", name = "Otro auto"),
                refuel = it.refuel.copy(vehicleId = "other", id = "other-fill")) }
            val plan = ImportPlan("other", newVehicleFuelType = "premium")
            assertEquals(ImportResult(1, 0), repo.import(original + other, plan))
            assertEquals("Otro auto", repo.dao.allVehicles().single().name)
            assertEquals("premium", repo.dao.allVehicles().single().fuelType)
            assertEquals("premium", repo.dao.allRefuels().single().fuelType)
            try { repo.import(original + other, plan); fail("Should choose existing target") } catch (_: IllegalArgumentException) { }
            assertEquals(1, repo.dao.allRefuels().size)
            val revision = repo.dao.revision()
            try { repo.import(original, ImportPlan(original.first().vehicle.id, "missing")); fail("Missing target") } catch (_: IllegalStateException) { }
            assertEquals(revision, repo.dao.revision())
        } finally { db.close() }
    }

    @Test fun resettingRoomAndAppearanceKeepsFreshInstallEmptyAfterInitialization() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val db = Room.inMemoryDatabaseBuilder(context, FuelDatabase::class.java).build()
        val appearance = mx.fuelstatus.backup.AppearanceStore(context, "test-reset-appearance")
        try {
            val repo = FuelRepository(db); repo.initialize(); repo.import(FuelCsv.parse(csv()))
            val vehicle = repo.dao.allVehicles().single()
            repo.save(Expense(vehicleId = vehicle.id, timestamp = 1000, category = "Servicio", costCents = 50000))
            appearance.update { Preferences("dark", false, vehicle.id) }
            val source = mx.fuelstatus.backup.RoomBackupSource(repo, appearance)
            source.reset(); repo.initialize()
            assertTrue(repo.dao.allVehicles().isEmpty()); assertTrue(repo.dao.allRefuels().isEmpty()); assertTrue(repo.dao.allExpenses().isEmpty())
            assertEquals(Preferences(), appearance.state.value)
            assertEquals(1L, repo.dao.revision())
            assertTrue(source.snapshot("new-phone", 1000).vehicles.isEmpty())
        } finally { db.close(); context.getSharedPreferences("test-reset-appearance", 0).edit().clear().commit() }
    }

    @Test fun creatingCsvVehicleNeverOverwritesAnExistingVehicleWithTheSameSourceId() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val db = Room.inMemoryDatabaseBuilder(context, FuelDatabase::class.java).build()
        try {
            val repo = FuelRepository(db); repo.initialize()
            val rows = FuelCsv.parse(csv())
            val existing = rows.first().vehicle.copy(name = "Vehículo renombrado", fuelType = "diesel")
            repo.save(existing)
            repo.import(rows, ImportPlan(rows.first().vehicle.id, newVehicleFuelType = "premium"))
            assertEquals(existing, repo.dao.allVehicles().first { it.id == existing.id })
            assertEquals(2, repo.dao.allVehicles().size)
            assertTrue(repo.dao.allRefuels().all { it.vehicleId != existing.id && it.fuelType == "premium" })
        } finally { db.close() }
    }

}
