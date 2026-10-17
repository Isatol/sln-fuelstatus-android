package mx.fuelstatus.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Dao
interface FuelDao {
    @Query("SELECT * FROM vehicles ORDER BY year DESC, name") fun vehicles(): Flow<List<Vehicle>>
    @Query("SELECT * FROM vehicles") suspend fun allVehicles(): List<Vehicle>
    @Query("SELECT * FROM refuels ORDER BY timestamp DESC, odometerMeters DESC") fun refuels(): Flow<List<Refuel>>
    @Query("SELECT * FROM refuels") suspend fun allRefuels(): List<Refuel>
    @Query("SELECT * FROM expenses ORDER BY timestamp DESC") fun expenses(): Flow<List<Expense>>
    @Query("SELECT * FROM expenses") suspend fun allExpenses(): List<Expense>
    @Query("SELECT * FROM expense_categories ORDER BY CASE WHEN name = 'Otros' THEN 1 ELSE 0 END, sortOrder, name COLLATE NOCASE") fun expenseCategories(): Flow<List<ExpenseCategory>>
    @Query("SELECT * FROM expense_categories ORDER BY CASE WHEN name = 'Otros' THEN 1 ELSE 0 END, sortOrder, name COLLATE NOCASE") suspend fun allExpenseCategories(): List<ExpenseCategory>
    @Query("DELETE FROM expense_categories") suspend fun deleteAllExpenseCategories()
    @Insert(onConflict = OnConflictStrategy.IGNORE) suspend fun initializeCategories(values: List<ExpenseCategory>)
    @Insert suspend fun insertExpenseCategories(values: List<ExpenseCategory>)
    @Query("SELECT * FROM payment_methods ORDER BY CASE WHEN id = 'cash' THEN 0 ELSE 1 END, name COLLATE NOCASE") fun paymentMethods(): Flow<List<PaymentMethod>>
    @Query("SELECT * FROM payment_methods ORDER BY CASE WHEN id = 'cash' THEN 0 ELSE 1 END, name COLLATE NOCASE") suspend fun allPaymentMethods(): List<PaymentMethod>
    @Query("SELECT revision FROM backup_revision WHERE id = 1") suspend fun revision(): Long?
    @Insert(onConflict = OnConflictStrategy.IGNORE) suspend fun initializeRevision(value: BackupRevision)
    @Query("UPDATE backup_revision SET revision = revision + 1 WHERE id = 1") suspend fun changed()
    @Query("DELETE FROM refuels") suspend fun deleteAllRefuels()
    @Query("DELETE FROM expenses") suspend fun deleteAllExpenses()
    @Query("DELETE FROM backup_revision") suspend fun deleteRevision()
    @Query("DELETE FROM vehicles") suspend fun deleteAllVehicles()
    @Query("DELETE FROM payment_methods") suspend fun deleteAllPaymentMethods()
    @Insert suspend fun insertVehicles(values: List<Vehicle>)
    @Insert suspend fun insertExpenses(values: List<Expense>)
    @Insert suspend fun insertPaymentMethods(values: List<PaymentMethod>)
    @Insert(onConflict = OnConflictStrategy.IGNORE) suspend fun initializeCash(value: PaymentMethod)
    @Insert suspend fun insertPaymentMethod(value: PaymentMethod)
    @Upsert suspend fun save(vehicle: Vehicle)
    @Upsert suspend fun save(refuel: Refuel)
    @Upsert suspend fun save(expense: Expense)
    @Insert(onConflict = OnConflictStrategy.IGNORE) suspend fun insertRefuels(rows: List<Refuel>): List<Long>
    @Delete suspend fun delete(refuel: Refuel)
    @Delete suspend fun delete(expense: Expense)
}

@Database(entities = [Vehicle::class, Refuel::class, Expense::class, BackupRevision::class, PaymentMethod::class, ExpenseCategory::class], version = 6, exportSchema = true)
abstract class FuelDatabase : RoomDatabase() {
    abstract fun dao(): FuelDao
    companion object {
        val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE expenses ADD COLUMN paymentMethodId TEXT DEFAULT NULL REFERENCES payment_methods(id) ON UPDATE NO ACTION ON DELETE RESTRICT")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_expenses_paymentMethodId ON expenses(paymentMethodId)")
                db.execSQL("UPDATE backup_revision SET revision = revision + 1 WHERE id = 1")
            }
        }

        val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("CREATE TABLE IF NOT EXISTS expense_categories (name TEXT NOT NULL, icon TEXT NOT NULL, sortOrder INTEGER NOT NULL, PRIMARY KEY(name))")
                val names = db.query("SELECT DISTINCT category FROM expenses").use { cursor ->
                    buildList { while (cursor.moveToNext()) add(cursor.getString(0)) }
                }
                ExpenseCategory.forLegacyExpenses(names).forEach { category ->
                    db.execSQL("INSERT INTO expense_categories (name, icon, sortOrder) VALUES (?, ?, ?)",
                        arrayOf<Any>(category.name, category.icon, category.sortOrder))
                }
                // The next local/Drive snapshot must include the new catalog even without a new expense.
                db.execSQL("UPDATE backup_revision SET revision = revision + 1 WHERE id = 1")
            }
        }

        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("CREATE TABLE IF NOT EXISTS payment_methods (id TEXT NOT NULL, name TEXT NOT NULL, brand TEXT NOT NULL, PRIMARY KEY(id))")
                db.execSQL("INSERT INTO payment_methods (id, name, brand) VALUES ('cash', 'Efectivo', 'cash')")
                db.execSQL("ALTER TABLE refuels ADD COLUMN paymentMethodId TEXT DEFAULT NULL REFERENCES payment_methods(id) ON UPDATE NO ACTION ON DELETE RESTRICT")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_refuels_paymentMethodId ON refuels(paymentMethodId)")
            }
        }

        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE vehicles ADD COLUMN fuelType TEXT NOT NULL DEFAULT 'magna'")
                db.execSQL("ALTER TABLE refuels ADD COLUMN fuelType TEXT NOT NULL DEFAULT 'unspecified'")
            }
        }

        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("CREATE TABLE IF NOT EXISTS backup_revision (id INTEGER NOT NULL, revision INTEGER NOT NULL, PRIMARY KEY(id))")
                db.execSQL("INSERT INTO backup_revision (id, revision) VALUES (1, 1)")
            }
        }
    }
}
