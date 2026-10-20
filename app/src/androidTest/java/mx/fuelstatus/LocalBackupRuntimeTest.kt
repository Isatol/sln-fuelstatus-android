package mx.fuelstatus

import android.content.Context
import android.content.ContextWrapper
import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.ExistingWorkPolicy
import androidx.work.WorkInfo
import androidx.work.WorkManager
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import mx.fuelstatus.backup.*
import mx.fuelstatus.data.*
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.util.UUID
import java.util.concurrent.TimeUnit

@RunWith(AndroidJUnit4::class)
class LocalBackupRuntimeTest {
    @Test fun upgradeRetiresPreviousJobsAndKeepsHistoryPreferencesAndBackup() = runBlocking {
        val base = InstrumentationRegistry.getInstrumentation().targetContext
        val prefix = "test-local-${UUID.randomUUID()}-"
        val directory = File(base.cacheDir, prefix).apply { mkdirs() }
        val context = object : ContextWrapper(base) {
            override fun getFilesDir(): File = directory
            override fun getSharedPreferences(name: String, mode: Int) = base.getSharedPreferences(prefix + name, mode)
        }
        val database = Room.inMemoryDatabaseBuilder(context, FuelDatabase::class.java).build()
        val manager = WorkManager.getInstance(base)
        try {
            val repository = FuelRepository(database); repository.initialize()
            val vehicle = Vehicle("car", "Auto conservado", "Sedán", 2024)
            repository.save(vehicle)
            repository.save(Refuel(vehicleId = vehicle.id, timestamp = 1000, odometerMeters = 1000000, volumeMl = 40000, costCents = 100000))
            repository.save(Expense(vehicleId = vehicle.id, timestamp = 1000, category = "Servicio", costCents = 50000))
            repository.addPaymentMethod(PaymentMethod("card", "Tarjeta conservada", "visa"))
            repository.addExpenseCategory("Llantas", "tire_repair")
            val appearance = AppearanceStore(context)
            appearance.update { Preferences("dark", false, vehicle.id, "violet", "blue") }
            val source = RoomBackupSource(repository, appearance)
            val before = source.snapshot("previous-device", 1234)
            val local = LocalBackupStore(File(directory, "backups"))
            local.write(LocalBackup(before, BackupCodec.encode(before)))
            val legacy = context.getSharedPreferences("drive_backup", 0)
            legacy.edit().putString("device_id", "previous-device")
                .putString("status", "{\"email\":\"test@example.com\",\"folderId\":\"synthetic-folder\",\"phase\":\"pending\"}").commit()
            val job = OneTimeWorkRequestBuilder<DriveBackupWorker>().setInitialDelay(1, TimeUnit.DAYS).build()
            manager.enqueueUniqueWork(BackupRuntime.LEGACY_WORK_NAME, ExistingWorkPolicy.REPLACE, job).result.get()

            val runtime = BackupRuntime(context, repository, appearance)
            withTimeout(10000) {
                while (manager.getWorkInfoById(job.id).get()?.state != WorkInfo.State.CANCELLED) delay(50)
            }
            assertEquals(before, runtime.engine.refreshLocal().document)
            assertEquals(before, source.snapshot("previous-device", 1234))
            assertEquals(before, local.latest()!!.document)
            assertTrue(legacy.all.isEmpty())
            assertEquals("previous-device", context.getSharedPreferences("local_backup", 0).getString("device_id", null))
            val permissions = base.packageManager.getPackageInfo(base.packageName, android.content.pm.PackageManager.GET_PERMISSIONS).requestedPermissions.orEmpty()
            assertFalse(permissions.contains("android.permission.INTERNET"))
            assertFalse(permissions.contains("android.permission.ACCESS_NETWORK_STATE"))
        } finally {
            manager.cancelUniqueWork(BackupRuntime.LEGACY_WORK_NAME).result.get()
            database.close()
            listOf("appearance", "drive_backup", "local_backup").forEach { context.getSharedPreferences(it, 0).edit().clear().commit() }
            directory.deleteRecursively()
        }
    }
}
