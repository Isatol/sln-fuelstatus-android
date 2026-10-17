package mx.fuelstatus

import android.app.Application
import androidx.room.Room
import mx.fuelstatus.backup.AppearanceStore
import mx.fuelstatus.backup.BackupRuntime
import mx.fuelstatus.data.FuelDatabase
import mx.fuelstatus.data.FuelRepository

class FuelApplication : Application() {
    val graph: FuelGraph by lazy { FuelGraph(this) }
}

class FuelGraph(application: Application) {
    val repository = FuelRepository(Room.databaseBuilder(application, FuelDatabase::class.java, "fuelstatus.db")
        .addMigrations(FuelDatabase.MIGRATION_1_2, FuelDatabase.MIGRATION_2_3, FuelDatabase.MIGRATION_3_4, FuelDatabase.MIGRATION_4_5, FuelDatabase.MIGRATION_5_6).build())
    val appearance = AppearanceStore(application)
    val backup = BackupRuntime(application, repository, appearance)
}
