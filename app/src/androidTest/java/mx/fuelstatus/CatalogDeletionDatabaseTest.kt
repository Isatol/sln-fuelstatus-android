package mx.fuelstatus

import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.runBlocking
import mx.fuelstatus.backup.*
import mx.fuelstatus.data.*
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class CatalogDeletionDatabaseTest {
    private val context get() = InstrumentationRegistry.getInstrumentation().targetContext

    @Test fun deletingCardReassignsBothKindsOfRecordsAcrossVehiclesAndSurvivesRestore() = runBlocking {
        val db = Room.inMemoryDatabaseBuilder(context, FuelDatabase::class.java).build()
        val prefsName = "test-catalog-deletion"
        try {
            val repo = FuelRepository(db); repo.initialize()
            val card = PaymentMethod("card", "Personal", "visa")
            val keep = card.copy(id = "keep", name = "Conservar")
            repo.addPaymentMethod(card); repo.addPaymentMethod(keep)
            listOf("car1", "car2").forEach { repo.save(Vehicle(it, it, "Sedán", 2024)) }
            val fills = listOf(
                Refuel("fill1", "car1", 1000, 10000, 20000, 50000, paymentMethodId = card.id, notes = "Original"),
                Refuel("fill2", "car2", 2000, 20000, 30000, 70000, paymentMethodId = card.id),
                Refuel("fill3", "car1", 3000, 30000, 10000, 30000, paymentMethodId = keep.id),
                Refuel("fill4", "car2", 4000, 40000, 10000, 30000))
            val costs = listOf(
                Expense("cost1", "car1", 1000, "Peaje", 12345, "Nota", card.id),
                Expense("cost2", "car2", 1000, "Servicio", 45678, paymentMethodId = card.id),
                Expense("cost3", "car1", 1000, "Otros", 100, paymentMethodId = keep.id),
                Expense("cost4", "car2", 1000, "Otros", 200))
            fills.forEach { repo.save(it) }; costs.forEach { repo.save(it) }
            val before = repo.dao.revision()!!
            repo.deletePaymentMethod(card.id)
            assertEquals(before + 1, repo.dao.revision())
            val expectedFills = fills.map { if (it.paymentMethodId == card.id) it.copy(paymentMethodId = "cash") else it }.sortedBy { it.id }
            val expectedCosts = costs.map { if (it.paymentMethodId == card.id) it.copy(paymentMethodId = "cash") else it }.sortedBy { it.id }
            assertEquals(expectedFills, repo.dao.allRefuels().sortedBy { it.id })
            assertEquals(expectedCosts, repo.dao.allExpenses().sortedBy { it.id })
            assertEquals(listOf(PaymentMethod.Cash, keep), repo.dao.allPaymentMethods())
            val source = RoomBackupSource(repo, AppearanceStore(context, prefsName))
            val snapshot = BackupCodec.decode(BackupCodec.encode(source.snapshot("phone", 1000)))
            source.reset(); source.restore(snapshot); repo.initialize()
            assertEquals(expectedFills, repo.dao.allRefuels().sortedBy { it.id })
            assertEquals(expectedCosts, repo.dao.allExpenses().sortedBy { it.id })
            assertEquals(snapshot.paymentMethods, repo.dao.allPaymentMethods())
        } finally { db.close(); context.getSharedPreferences(prefsName, 0).edit().clear().commit() }
    }

    @Test fun deletedDefaultAndCustomCategoriesStayDeletedOnReopenAndRestore() = runBlocking {
        val name = "fuelstatus-catalog-deletion.db"
        context.deleteDatabase(name)
        var db = Room.databaseBuilder(context, FuelDatabase::class.java, name).build()
        val prefsName = "test-category-deletion"
        try {
            var repo = FuelRepository(db); repo.initialize()
            repo.addExpenseCategory("Llantas", CategoryIcon.Tire.code)
            listOf("car1", "car2").forEach { repo.save(Vehicle(it, it, "Sedán", 2024)) }
            val costs = listOf(Expense("one", "car1", 1000, "Servicio", 12345, "Original"),
                Expense("two", "car2", 2000, "Llantas", 67890, "Original"),
                Expense("three", "car2", 3000, "Otros", 123))
            costs.forEach { repo.save(it) }
            repo.deleteExpenseCategory("Servicio"); repo.deleteExpenseCategory("Llantas")
            val expected = costs.map { it.copy(category = "Otros") }.sortedBy { it.id }
            assertEquals(expected, repo.dao.allExpenses().sortedBy { it.id })
            db.close(); db = Room.databaseBuilder(context, FuelDatabase::class.java, name).build()
            repo = FuelRepository(db); repo.initialize()
            assertFalse(repo.dao.allExpenseCategories().any { it.name in listOf("Servicio", "Llantas") })
            val source = RoomBackupSource(repo, AppearanceStore(context, prefsName))
            val snapshot = BackupCodec.decode(BackupCodec.encode(source.snapshot("phone", 1000)))
            source.reset(); source.restore(snapshot); repo.initialize()
            assertEquals(snapshot.expenseCategories, repo.dao.allExpenseCategories())
            assertEquals(expected, repo.dao.allExpenses().sortedBy { it.id })
            repo.dao.allExpenseCategories().filter { it.name != "Otros" }.forEach { repo.deleteExpenseCategory(it.name) }
            repo.initialize()
            assertEquals(listOf(ExpenseCategory.Other), repo.dao.allExpenseCategories())
            source.reset(); repo.initialize()
            assertEquals(ExpenseCategory.Defaults, repo.dao.allExpenseCategories())
        } finally { db.close(); context.deleteDatabase(name); context.getSharedPreferences(prefsName, 0).edit().clear().commit() }
    }

    @Test fun protectedAndMissingEntriesDoNotChangeDataOrRevisionAndUnusedEntriesCanBeDeleted() = runBlocking {
        val db = Room.inMemoryDatabaseBuilder(context, FuelDatabase::class.java).build()
        try {
            val repo = FuelRepository(db); repo.initialize()
            val revision = repo.dao.revision()
            for (id in listOf("cash", "missing")) {
                try { repo.deletePaymentMethod(id); fail("Protected or missing method") } catch (_: IllegalArgumentException) { }
            }
            for (name in listOf("Otros", "missing")) {
                try { repo.deleteExpenseCategory(name); fail("Protected or missing category") } catch (_: IllegalArgumentException) { }
            }
            repo.dao.deletePaymentMethod("cash"); repo.dao.deleteExpenseCategory("Otros")
            assertEquals(revision, repo.dao.revision())
            assertEquals(listOf(PaymentMethod.Cash), repo.dao.allPaymentMethods())
            assertEquals(ExpenseCategory.Defaults, repo.dao.allExpenseCategories())
            repo.addPaymentMethod(PaymentMethod("unused", "Sin uso", "amex"))
            repo.deletePaymentMethod("unused"); repo.deleteExpenseCategory("Peaje")
            assertEquals(listOf(PaymentMethod.Cash), repo.dao.allPaymentMethods())
            assertFalse(repo.dao.allExpenseCategories().any { it.name == "Peaje" })
        } finally { db.close() }
    }

    @Test fun failureDuringDeletionRollsBackReassignmentsAndBackupRevision() = runBlocking {
        val db = Room.inMemoryDatabaseBuilder(context, FuelDatabase::class.java).build()
        try {
            val repo = FuelRepository(db); repo.initialize()
            repo.save(Vehicle("car", "Auto", "Sedán", 2024))
            repo.addPaymentMethod(PaymentMethod("card", "Personal", "visa"))
            val fill = Refuel("fill", "car", 1000, 10000, 20000, 50000, paymentMethodId = "card")
            val cost = Expense("cost", "car", 1000, "Peaje", 12345, paymentMethodId = "card")
            repo.save(fill); repo.save(cost)
            val revision = repo.dao.revision()
            db.openHelper.writableDatabase.execSQL("CREATE TRIGGER prevent_card_delete BEFORE DELETE ON payment_methods BEGIN SELECT RAISE(ABORT, 'test failure'); END")
            try { repo.deletePaymentMethod("card"); fail("Expected rollback") } catch (_: android.database.sqlite.SQLiteException) { }
            assertEquals(listOf(fill), repo.dao.allRefuels()); assertEquals(listOf(cost), repo.dao.allExpenses())
            assertEquals(revision, repo.dao.revision())
            db.openHelper.writableDatabase.execSQL("CREATE TRIGGER prevent_category_delete BEFORE DELETE ON expense_categories BEGIN SELECT RAISE(ABORT, 'test failure'); END")
            try { repo.deleteExpenseCategory("Peaje"); fail("Expected rollback") } catch (_: android.database.sqlite.SQLiteException) { }
            assertEquals(listOf(cost), repo.dao.allExpenses()); assertEquals(revision, repo.dao.revision())
        } finally { db.close() }
    }
}
