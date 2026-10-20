package mx.fuelstatus.backup

import android.content.Context
import androidx.room.withTransaction
import androidx.work.CoroutineWorker
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import mx.fuelstatus.Preferences
import mx.fuelstatus.domain.IconRingColor
import mx.fuelstatus.data.*
import java.io.File
import java.util.UUID

class AppearanceStore(context: Context, name: String = "appearance", private val onIconColor: (String) -> Unit = {}) {
    private val prefs = context.getSharedPreferences(name, 0)
    private val mutable = MutableStateFlow(Preferences(prefs.getString("theme", "system").orEmpty(),
        prefs.getBoolean("dynamic", true), prefs.getString("vehicle", "").orEmpty(),
        mx.fuelstatus.domain.ThemePalette.fromCode(prefs.getString("palette", null)).code,
        IconRingColor.fromCode(prefs.getString("icon_ring", null)).code))
    val state = mutable.asStateFlow()
    @Synchronized fun synchronizeIcon() { onIconColor(mutable.value.iconRingColor) }
    @Synchronized fun update(transform: (Preferences) -> Preferences) {
        val previous = mutable.value
        val value = transform(previous)
        val changedIcon = previous.iconRingColor != value.iconRingColor
        try {
            if (changedIcon) onIconColor(value.iconRingColor)
            check(prefs.edit().putString("theme", value.theme).putBoolean("dynamic", value.dynamicColor)
                .putString("vehicle", value.vehicleId).putString("palette", value.colorPalette)
                .putString("icon_ring", value.iconRingColor).commit()) { "No se pudieron guardar los ajustes." }
            mutable.value = value
        } catch (failure: Exception) {
            if (changedIcon) runCatching { onIconColor(previous.iconRingColor) }.onFailure(failure::addSuppressed)
            throw failure
        }
    }
}

class RoomBackupSource(private val repository: FuelRepository, private val appearance: AppearanceStore) : BackupSource {
    override suspend fun revision() = repository.dao.revision() ?: 1L
    override suspend fun snapshot(deviceId: String, now: Long): BackupDocument = repository.database.withTransaction {
        BackupDocument(now, deviceId, revision(), repository.dao.allVehicles(), repository.dao.allRefuels(),
            repository.dao.allExpenses(), appearance.state.value, repository.dao.allPaymentMethods(), repository.dao.allExpenseCategories())
    }
    private suspend fun replaceLocal(value: Preferences, change: suspend () -> Unit) {
        val previous = appearance.state.value
        try {
            repository.database.withTransaction {
                change()
                appearance.update { value }
            }
        } catch (failure: Exception) {
            if (appearance.state.value != previous) {
                runCatching { appearance.update { previous } }.onFailure(failure::addSuppressed)
            }
            throw failure
        }
    }
    override suspend fun reset() = replaceLocal(Preferences()) { repository.reset() }
    override suspend fun restore(document: BackupDocument) = replaceLocal(document.appearance) {
            val dao = repository.dao
            dao.deleteAllRefuels(); dao.deleteAllExpenses(); dao.deleteAllVehicles()
            dao.deleteAllPaymentMethods(); dao.insertPaymentMethods(document.paymentMethods)
            dao.deleteAllExpenseCategories(); dao.insertExpenseCategories(document.expenseCategories)
            dao.insertVehicles(document.vehicles); dao.insertRefuels(document.refuels); dao.insertExpenses(document.expenses)
            dao.initializeRevision(BackupRevision()); dao.changed()
    }
}

class BackupRuntime(context: Context, repository: FuelRepository, appearance: AppearanceStore) {
    private val preferences = context.getSharedPreferences("local_backup", 0)
    private val legacy = context.getSharedPreferences("drive_backup", 0)
    private val deviceId = preferences.getString("device_id", null)
        ?: legacy.getString("device_id", null)
        ?: UUID.randomUUID().toString()

    init {
        check(preferences.edit().putString("device_id", deviceId).commit()) { "No se pudo guardar el identificador de las copias locales." }
        // Cancel jobs retained by an update, without contacting any remote account.
        WorkManager.getInstance(context).cancelUniqueWork(LEGACY_WORK_NAME)
        check(legacy.edit().clear().commit()) { "No se pudo retirar la configuración anterior de copias." }
    }

    val engine = BackupEngine(RoomBackupSource(repository, appearance), LocalBackupStore(File(context.filesDir, "backups")), deviceId)

    companion object {
        const val LEGACY_WORK_NAME = "fuelstatus-drive-backup"
    }
}

// Keep this class name so persisted jobs can finish safely after an update.
class DriveBackupWorker(context: Context, parameters: WorkerParameters) : CoroutineWorker(context, parameters) {
    override suspend fun doWork(): Result = Result.success()
}
