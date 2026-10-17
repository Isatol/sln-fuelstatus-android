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
class CategoryDatabaseTest {
    private val context get() = InstrumentationRegistry.getInstrumentation().targetContext

    @Test fun migrationFourToFivePreservesEveryExpenseAndSeedsDistinctCategories() = runBlocking {
        val name = "fuelstatus-category-migration.db"
        context.deleteDatabase(name)
        val schemaText = InstrumentationRegistry.getInstrumentation().context.assets
            .open("mx.fuelstatus.data.FuelDatabase/4.json").bufferedReader().use { it.readText() }
        val schema = JsonParser.parseString(schemaText).asJsonObject.getAsJsonObject("database")
        context.openOrCreateDatabase(name, 0, null).use { old ->
            schema.getAsJsonArray("entities").forEach { entity ->
                val item = entity.asJsonObject; val table = item.get("tableName").asString
                old.execSQL(item.get("createSql").asString.replace("\${TABLE_NAME}", table))
                item.getAsJsonArray("indices")?.forEach { index -> old.execSQL(index.asJsonObject.get("createSql").asString.replace("\${TABLE_NAME}", table)) }
            }
            schema.getAsJsonArray("setupQueries").forEach { old.execSQL(it.asString) }
            old.execSQL("INSERT INTO vehicles VALUES ('car', 'Auto', 'Sedán', 2024, 'premium')")
            old.execSQL("INSERT INTO payment_methods VALUES ('cash', 'Efectivo', 'cash')")
            old.execSQL("INSERT INTO expenses VALUES ('service', 'car', 1000, 'Servicio', 12345, 'Original')")
            old.execSQL("INSERT INTO expenses VALUES ('custom', 'car', 1001, 'Categoría antigua', 67890, 'Conservar')")
            old.execSQL("INSERT INTO backup_revision VALUES (1, 9)")
            old.version = 4
        }
        val db = Room.databaseBuilder(context, FuelDatabase::class.java, name).addMigrations(FuelDatabase.MIGRATION_4_5, FuelDatabase.MIGRATION_5_6).build()
        try {
            val expenses = db.dao().allExpenses()
            assertEquals(2, expenses.size)
            assertEquals(67890L, expenses.single { it.id == "custom" }.costCents)
            assertEquals("Conservar", expenses.single { it.id == "custom" }.notes)
            val categories = db.dao().allExpenseCategories()
            assertTrue(categories.containsAll(ExpenseCategory.Defaults))
            assertEquals(CategoryIcon.Tag.code, categories.single { it.name == "Categoría antigua" }.icon)
            assertEquals("Otros", categories.last().name)
            assertEquals(11L, db.dao().revision())
            FuelRepository(db).save(expenses.single { it.id == "custom" }.copy(costCents = 70000))
            assertEquals(70000L, db.dao().allExpenses().single { it.id == "custom" }.costCents)
        } finally { db.close(); context.deleteDatabase(name) }
    }

    @Test fun catalogPersistsReopensRestoresAndResetsWithExpenses() = runBlocking {
        val name = "fuelstatus-category-persistence.db"
        context.deleteDatabase(name)
        var db = Room.databaseBuilder(context, FuelDatabase::class.java, name).build()
        val appearance = AppearanceStore(context, "test-category-appearance")
        try {
            var repo = FuelRepository(db); repo.initialize()
            assertEquals(ExpenseCategory.Defaults, repo.dao.allExpenseCategories())
            repo.addExpenseCategory(" Llantas ", CategoryIcon.Tire.code)
            repo.addExpenseCategory("Multas", CategoryIcon.Warning.code)
            val vehicle = Vehicle("car", "Auto", "Sedán", 2024); repo.save(vehicle)
            val expense = Expense("expense", vehicle.id, 1000, "Llantas", 12345, "Original"); repo.save(expense)
            val snapshot = RoomBackupSource(repo, appearance).snapshot("phone", 1000)
            assertEquals("Otros", snapshot.expenseCategories.last().name)
            assertEquals(listOf("Llantas", "Multas", "Otros"), snapshot.expenseCategories.takeLast(3).map { it.name })
            db.close(); db = Room.databaseBuilder(context, FuelDatabase::class.java, name).build(); repo = FuelRepository(db)
            repo.initialize()
            assertEquals(snapshot.expenseCategories, repo.dao.allExpenseCategories())
            assertEquals(listOf(expense), repo.dao.allExpenses())
            val source = RoomBackupSource(repo, appearance)
            source.reset(); repo.initialize()
            assertEquals(ExpenseCategory.Defaults, repo.dao.allExpenseCategories())
            assertTrue(repo.dao.allExpenses().isEmpty())
            source.restore(BackupCodec.decode(BackupCodec.encode(snapshot)))
            assertEquals(snapshot.expenseCategories, repo.dao.allExpenseCategories())
            assertEquals(snapshot.expenses, repo.dao.allExpenses())
        } finally { db.close(); context.deleteDatabase(name); context.getSharedPreferences("test-category-appearance", 0).edit().clear().commit() }
    }

    @Test fun invalidCategoriesAndNamesDoNotChangeDataOrBackupRevision() = runBlocking {
        val db = Room.inMemoryDatabaseBuilder(context, FuelDatabase::class.java).build()
        try {
            val repo = FuelRepository(db); repo.initialize()
            repo.addExpenseCategory("Batería", CategoryIcon.Battery.code)
            val revision = repo.dao.revision()
            for ((name, icon) in listOf("" to "build", "   " to "build", "sErViCiO" to "build", " batería " to "build",
                "bateria" to "build", "OTROS" to "build", "a".repeat(51) to "build", "Inválida" to "missing")) {
                try { repo.addExpenseCategory(name, icon); fail("Expected invalid category: $name") } catch (_: IllegalArgumentException) { }
            }
            try { repo.save(Expense(vehicleId = "missing", timestamp = 1000, category = "Inexistente", costCents = 100)); fail("Unknown category") }
            catch (_: IllegalArgumentException) { }
            assertEquals(revision, repo.dao.revision())
            assertEquals(8, repo.dao.allExpenseCategories().size)
        } finally { db.close() }
    }
}
