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
    @Query("SELECT revision FROM backup_revision WHERE id = 1") suspend fun revision(): Long?
    @Insert(onConflict = OnConflictStrategy.IGNORE) suspend fun initializeRevision(value: BackupRevision)
    @Query("UPDATE backup_revision SET revision = revision + 1 WHERE id = 1") suspend fun changed()
    @Query("DELETE FROM refuels") suspend fun deleteAllRefuels()
    @Query("DELETE FROM expenses") suspend fun deleteAllExpenses()
    @Query("DELETE FROM backup_revision") suspend fun deleteRevision()
    @Query("DELETE FROM vehicles") suspend fun deleteAllVehicles()
    @Insert suspend fun insertVehicles(values: List<Vehicle>)
    @Insert suspend fun insertExpenses(values: List<Expense>)
    @Upsert suspend fun save(vehicle: Vehicle)
    @Upsert suspend fun save(refuel: Refuel)
    @Upsert suspend fun save(expense: Expense)
    @Insert(onConflict = OnConflictStrategy.IGNORE) suspend fun insertRefuels(rows: List<Refuel>): List<Long>
    @Delete suspend fun delete(refuel: Refuel)
    @Delete suspend fun delete(expense: Expense)
}

@Database(entities = [Vehicle::class, Refuel::class, Expense::class, BackupRevision::class], version = 3, exportSchema = true)
abstract class FuelDatabase : RoomDatabase() {
    abstract fun dao(): FuelDao
    companion object {
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
