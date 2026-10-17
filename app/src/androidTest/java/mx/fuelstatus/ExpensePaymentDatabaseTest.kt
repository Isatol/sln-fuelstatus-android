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
class ExpensePaymentDatabaseTest {
    private val context get() = InstrumentationRegistry.getInstrumentation().targetContext

    @Test fun migrationFiveToSixPreservesExpensesAndLeavesHistoricalPaymentUnknown() = runBlocking {
        val name = "fuelstatus-expense-payment-migration.db"
        context.deleteDatabase(name)
        val schemaText = InstrumentationRegistry.getInstrumentation().context.assets
            .open("mx.fuelstatus.data.FuelDatabase/5.json").bufferedReader().use { it.readText() }
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
            old.execSQL("INSERT INTO payment_methods VALUES ('card', 'Visa personal', 'visa')")
            old.execSQL("INSERT INTO expense_categories VALUES ('Servicio', 'build', 0)")
            old.execSQL("INSERT INTO expenses VALUES ('expense', 'car', 1000, 'Servicio', 12345, 'Original')")
            old.execSQL("INSERT INTO refuels VALUES ('fill', 'car', 1000, 1000000, 40000, 100000, 1, 1, 0, '', '', 'premium', 'card')")
            old.execSQL("INSERT INTO backup_revision VALUES (1, 9)")
            old.version = 5
        }
        val db = Room.databaseBuilder(context, FuelDatabase::class.java, name).addMigrations(FuelDatabase.MIGRATION_5_6).build()
        try {
            val expense = db.dao().allExpenses().single()
            assertEquals(Expense("expense", "car", 1000, "Servicio", 12345, "Original"), expense)
            assertNull(expense.paymentMethodId)
            assertEquals("card", db.dao().allRefuels().single().paymentMethodId)
            assertEquals(10L, db.dao().revision())
            val repo = FuelRepository(db)
            repo.save(expense.copy(paymentMethodId = "card"))
            assertEquals("card", db.dao().allExpenses().single().paymentMethodId)
        } finally { db.close(); context.deleteDatabase(name) }
    }

    @Test fun assignmentsSurviveReopenRestoreAndResetAndInvalidMethodRollsBack() = runBlocking {
        val name = "fuelstatus-expense-payment-persistence.db"
        context.deleteDatabase(name)
        var db = Room.databaseBuilder(context, FuelDatabase::class.java, name).build()
        val appearance = AppearanceStore(context, "test-expense-payment-appearance")
        try {
            var repo = FuelRepository(db); repo.initialize()
            val card = PaymentMethod("card", "Tarjeta gasolina", "mastercard"); repo.addPaymentMethod(card)
            val vehicle = Vehicle("car", "Auto", "Sedán", 2024); repo.save(vehicle)
            val expense = Expense("expense", vehicle.id, 1000, "Servicio", 12345, "Original", card.id)
            repo.save(expense)
            val snapshot = RoomBackupSource(repo, appearance).snapshot("phone", 1000)
            try { repo.save(expense.copy(paymentMethodId = "missing")); fail("Missing payment method") }
            catch (_: android.database.sqlite.SQLiteConstraintException) { }
            assertEquals(snapshot.revision, repo.dao.revision())
            assertEquals(listOf(expense), repo.dao.allExpenses())
            try { repo.dao.deleteAllPaymentMethods(); fail("Referenced payment method") }
            catch (_: android.database.sqlite.SQLiteConstraintException) { }
            db.close(); db = Room.databaseBuilder(context, FuelDatabase::class.java, name).build(); repo = FuelRepository(db)
            assertEquals(listOf(expense), repo.dao.allExpenses())
            repo.save(expense.copy(paymentMethodId = PaymentMethod.Cash.id))
            assertEquals(PaymentMethod.Cash.id, repo.dao.allExpenses().single().paymentMethodId)
            repo.save(expense.copy(paymentMethodId = null))
            assertNull(repo.dao.allExpenses().single().paymentMethodId)
            val source = RoomBackupSource(repo, appearance)
            source.reset()
            assertTrue(repo.dao.allExpenses().isEmpty())
            assertEquals(listOf(PaymentMethod.Cash), repo.dao.allPaymentMethods())
            source.restore(BackupCodec.decode(BackupCodec.encode(snapshot)))
            assertEquals(snapshot.expenses, repo.dao.allExpenses())
            assertEquals(snapshot.paymentMethods, repo.dao.allPaymentMethods())
            source.reset(); repo.initialize()
            assertTrue(repo.dao.allExpenses().isEmpty())
        } finally { db.close(); context.deleteDatabase(name); context.getSharedPreferences("test-expense-payment-appearance", 0).edit().clear().commit() }
    }
}
