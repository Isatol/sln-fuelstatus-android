package mx.fuelstatus.backup

import android.accounts.Account
import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import androidx.room.withTransaction
import androidx.work.*
import com.google.android.gms.auth.api.identity.*
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.common.api.Scope
import com.google.gson.GsonBuilder
import com.google.gson.InstanceCreator
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.tasks.await
import mx.fuelstatus.FuelApplication
import mx.fuelstatus.Preferences
import mx.fuelstatus.data.*
import java.io.File
import java.util.UUID
import java.util.concurrent.TimeUnit

const val DRIVE_SCOPE = "https://www.googleapis.com/auth/drive.file"

class AppearanceStore(context: Context, name: String = "appearance") {
    private val prefs = context.getSharedPreferences(name, 0)
    private val mutable = MutableStateFlow(Preferences(prefs.getString("theme", "system").orEmpty(),
        prefs.getBoolean("dynamic", true), prefs.getString("vehicle", "").orEmpty()))
    val state = mutable.asStateFlow()
    @Synchronized fun update(transform: (Preferences) -> Preferences) {
        val value = transform(mutable.value)
        check(prefs.edit().putString("theme", value.theme).putBoolean("dynamic", value.dynamicColor)
            .putString("vehicle", value.vehicleId).commit()) { "No se pudieron guardar los ajustes." }
        mutable.value = value
    }
}

class PersistentSyncState(context: Context) : SyncStateStore {
    private val prefs = context.getSharedPreferences("drive_backup", 0)
    private val gson = GsonBuilder().registerTypeAdapter(SyncState::class.java, InstanceCreator { SyncState() }).create()
    private val mutable = MutableStateFlow(runCatching {
        gson.fromJson(prefs.getString("status", "{}"), SyncState::class.java) ?: SyncState()
    }.getOrDefault(SyncState()).let {
        if (it.phase == "resetting") it.copy(phase = "reset_failed", errorTitle = "El borrado se interrumpió",
            errorDetail = "Repite Borrar datos para completar la operación. La sincronización automática está pausada.")
        else if (it.running) it.copy(phase = if (it.connected) "pending" else "disconnected") else it
    })
    override val state = mutable.asStateFlow()
    val deviceId: String = prefs.getString("device_id", null) ?: UUID.randomUUID().toString().also {
        check(prefs.edit().putString("device_id", it).commit()) { "No se pudo inicializar el identificador del dispositivo." }
    }
    @Synchronized override fun update(transform: (SyncState) -> SyncState) {
        val value = transform(mutable.value)
        check(prefs.edit().putString("status", gson.toJson(value)).commit()) { "No se pudo guardar el estado de las copias." }
        mutable.value = value
    }
}

class RoomBackupSource(private val repository: FuelRepository, private val appearance: AppearanceStore) : BackupSource {
    override suspend fun revision() = repository.dao.revision() ?: 1L
    override suspend fun snapshot(deviceId: String, now: Long): BackupDocument = repository.database.withTransaction {
        BackupDocument(now, deviceId, revision(), repository.dao.allVehicles(), repository.dao.allRefuels(),
            repository.dao.allExpenses(), appearance.state.value)
    }
    override suspend fun reset() {
        repository.reset()
        appearance.update { Preferences() }
    }
    override suspend fun restore(document: BackupDocument) {
        repository.database.withTransaction {
            val dao = repository.dao
            dao.deleteAllRefuels(); dao.deleteAllExpenses(); dao.deleteAllVehicles()
            dao.insertVehicles(document.vehicles); dao.insertRefuels(document.refuels); dao.insertExpenses(document.expenses)
            dao.initializeRevision(BackupRevision()); dao.changed()
        }
        appearance.update { document.appearance }
    }
}

object GoogleDriveAuthorization {
    fun request(email: String? = null, chooseAccount: Boolean = false): AuthorizationRequest = AuthorizationRequest.builder()
        .setRequestedScopes(listOf(Scope(DRIVE_SCOPE)))
        .apply {
            if (!email.isNullOrBlank()) setAccount(Account(email, "com.google"))
            if (chooseAccount) setPrompt(AuthorizationRequest.Prompt.SELECT_ACCOUNT)
        }.build()

    suspend fun token(context: Context, email: String): String {
        try {
            val result = Identity.getAuthorizationClient(context).authorize(request(email)).await()
            if (result.hasResolution()) throw DriveFailure("Vuelve a autorizar Google Drive", "Google necesita que confirmes el acceso. Pulsa Reconectar en Ajustes.", authRequired = true)
            return result.accessToken ?: throw DriveFailure("No se obtuvo acceso a Google Drive", "Google no devolvió un token de acceso. Reconecta tu cuenta.", authRequired = true)
        } catch (e: ApiException) { throw failure(e) }
    }

    fun failure(error: Exception): DriveFailure {
        if (error is DriveFailure) return error
        val status = (error as? ApiException)?.statusCode
        val detail = "${if (status == null) "" else "Google Identity · código $status\n"}${error.message.orEmpty()}".take(4000)
        return DriveFailure(if (status == 10) "Revisa el cliente OAuth de FuelStatus en Google Cloud" else "No se pudo autorizar Google Drive",
            detail + if (status == 10) "\nComprueba mx.fuelstatus, el SHA-1 de la firma y la cuenta de prueba." else "", authRequired = true)
    }
}

class BackupRuntime(private val context: Context, repository: FuelRepository, appearance: AppearanceStore) {
    private val store = PersistentSyncState(context)
    private val manager get() = WorkManager.getInstance(context)
    val engine = BackupEngine(RoomBackupSource(repository, appearance), LocalBackupStore(File(context.filesDir, "backups")),
        store, store.deviceId,
        tokenProvider = { GoogleDriveAuthorization.token(context, it) },
        driveFactory = { DriveApi(GoogleDriveTransport(it), it) },
        online = { isOnline(context) },
        enqueue = { force ->
            val request = OneTimeWorkRequestBuilder<DriveBackupWorker>()
                .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
                .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.SECONDS)
                .setInputData(workDataOf("force" to force)).build()
            // A save can arrive just as the previous worker finishes. Append so that request is not lost.
            manager.enqueueUniqueWork(WORK_NAME, ExistingWorkPolicy.APPEND_OR_REPLACE, request)
        },
        cancelWork = { manager.cancelUniqueWork(WORK_NAME) },
        invalidateToken = { token -> Identity.getAuthorizationClient(context).clearToken(ClearTokenRequest.builder().setToken(token).build()).await() })

    companion object {
        const val WORK_NAME = "fuelstatus-drive-backup"
        fun isOnline(context: Context): Boolean {
            val manager = context.getSystemService(ConnectivityManager::class.java)
            val capabilities = manager.getNetworkCapabilities(manager.activeNetwork) ?: return false
            return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
                capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
        }
    }
}

class DriveBackupWorker(context: Context, parameters: WorkerParameters) : CoroutineWorker(context, parameters) {
    override suspend fun doWork(): Result {
        val engine = (applicationContext as FuelApplication).graph.backup.engine
        return try {
            when (engine.synchronize(inputData.getBoolean("force", false))) {
                SyncOutcome.Success -> Result.success()
                SyncOutcome.Retry -> if (runAttemptCount < 5) Result.retry() else Result.failure()
                SyncOutcome.NeedsAction -> Result.failure()
            }
        } catch (e: CancellationException) { throw e }
        catch (e: Exception) { engine.failed(e); Result.failure() }
    }
}
