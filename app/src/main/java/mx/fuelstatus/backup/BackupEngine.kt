package mx.fuelstatus.backup

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.io.IOException

/** All copies are immutable on Drive. Local revision counters are committed with each database change. */
class BackupEngine(
    private val source: BackupSource,
    private val local: LocalBackupStore,
    private val store: SyncStateStore,
    private val deviceId: String,
    private val tokenProvider: suspend (String) -> String,
    private val driveFactory: (String) -> DriveGateway,
    private val online: () -> Boolean,
    private val enqueue: (Boolean) -> Unit,
    private val cancelWork: () -> Unit,
    private val invalidateToken: suspend (String) -> Unit = {},
    private val now: () -> Long = System::currentTimeMillis,
) {
    @Volatile private var resetting = false
    private val localMutex = Mutex()
    private val syncMutex = Mutex()
    val state get() = store.state

    suspend fun refreshLocal(): LocalBackup = localMutex.withLock {
        try {
            val revision = source.revision()
            val previous = runCatching { local.latest() }.getOrNull()
            val backup = previous?.takeIf { it.document.revision == revision && it.document.deviceId == deviceId }
                ?: source.snapshot(deviceId, now()).let { LocalBackup(it, BackupCodec.encode(it)) }.also(local::write)
            store.update { it.copy(lastLocalAt = backup.document.createdAt, localRevision = backup.document.revision, localError = "") }
            backup
        } catch (e: CancellationException) { throw e }
        catch (e: Exception) {
            store.update { it.copy(localError = "No se pudo actualizar el archivo local: ${e.message.orEmpty()}") }
            throw e
        }
    }

    /** Database saves remain successful even if the extra file could not be written. UI retains the error. */
    suspend fun afterMutation() {
        try { refreshLocal() } catch (e: CancellationException) { throw e } catch (_: Exception) { return }
        requestSync()
    }

    suspend fun onForeground() {
        try { refreshLocal() } catch (e: CancellationException) { throw e } catch (_: Exception) { return }
        requestSync()
    }

    fun connecting() = store.update { it.copy(phase = "connecting") }
    fun canceled() = store.update { it.copy(phase = if (it.connected) "pending" else "disconnected") }
    fun failed(error: Exception) = recordFailure(error)

    fun requestSync(force: Boolean = false) {
        val current = state.value
        if (resetting || current.phase == "reset_failed" || !current.connected || current.needsChoice || current.running || (current.phase == "auth_required" && !force)) return
        if (!current.pending && !force) return
        store.update { it.copy(phase = if (online()) "pending" else "offline") }
        enqueue(force)
    }

    suspend fun connect(token: String) = syncMutex.withLock {
        try {
            refreshLocal()
            val drive = driveFactory(token)
            val email = drive.email()
            val folderId = drive.folder()
            val latest = drive.backups(folderId).firstOrNull()
            val old = state.value
            val same = old.email.equals(email, true) && old.folderId == folderId
            store.update {
                it.copy(email = email, folderId = folderId, phase = "pending",
                    baselineId = if (same && latest != null) old.baselineId else "", cloudRevision = if (same && latest != null) old.cloudRevision else 0,
                    lastCloudAt = if (same) old.lastCloudAt else 0, candidateId = "", candidateAt = 0,
                    pendingUploadId = if (same) old.pendingUploadId else "", pendingUploadMd5 = if (same) old.pendingUploadMd5 else "",
                    pendingUploadRevision = if (same) old.pendingUploadRevision else 0,
                    errorTitle = "", errorDetail = "", errorAt = 0)
            }
            if (latest != null && latest.id != state.value.baselineId && !acknowledgePending(latest)) choice(latest)
            else if (state.value.pending) requestSync()
            else store.update { it.copy(phase = "synced") }
        } catch (e: CancellationException) { canceled(); throw e }
        catch (e: Exception) { recordFailure(e) }
    }

    suspend fun disconnect() {
        cancelWork()
        syncMutex.withLock {
            store.update { SyncState(lastLocalAt = it.lastLocalAt, localRevision = it.localRevision, localError = it.localError) }
        }
    }

    /** The caller must obtain explicit confirmation. Local data is kept if Drive cleanup fails. */
    suspend fun reset(deleteDrive: Boolean) {
        check(!resetting) { "Ya hay un borrado en curso." }
        resetting = true
        try {
            cancelWork()
            syncMutex.withLock {
                val current = state.value
                store.update { it.copy(phase = "resetting") }
                if (deleteDrive) {
                    check(current.connected) { "La sesión de Drive cambió. Revisa la cuenta antes de borrar." }
                    check(online()) { "Necesitas conexión para borrar las copias de Drive. Los datos locales se conservan." }
                    val drive = driveFactory(tokenProvider(current.email))
                    check(drive.email().equals(current.email, true)) { "La cuenta de Google cambió. Reconecta la cuenta antes de borrar." }
                    val copies = drive.backups(current.folderId)
                    var removed = 0
                    try {
                        for (copy in copies) { drive.trash(copy.id); removed++ }
                    } catch (e: CancellationException) { throw e }
                    catch (e: Exception) {
                        throw DriveFailure("No se completó el borrado de Drive",
                            "Se enviaron $removed de ${copies.size} copias a la papelera. Los datos locales se conservan. Repite Borrar datos para continuar.\n${detail(e)}")
                    }
                }
                localMutex.withLock {
                    source.reset()
                    local.clear()
                    store.update { SyncState() }
                }
                refreshLocal()
            }
        } catch (e: CancellationException) {
            store.update { it.copy(phase = "reset_failed", errorTitle = "El borrado se interrumpió",
                errorDetail = "Repite Borrar datos para completar la operación. La sincronización automática está pausada.", errorAt = now()) }
            throw e
        } catch (e: Exception) {
            recordFailure(e)
            store.update { it.copy(phase = "reset_failed") }
            throw e
        } finally { resetting = false }
    }

    suspend fun synchronize(force: Boolean = false): SyncOutcome = syncMutex.withLock {
        var token: String? = null
        try {
            val current = state.value
            if (resetting || current.phase in listOf("reset_failed", "resetting") || !current.connected || current.needsChoice) return@withLock SyncOutcome.NeedsAction
            if (!online()) { store.update { it.copy(phase = "offline") }; return@withLock SyncOutcome.Retry }
            val backup = refreshLocal()
            store.update { it.copy(phase = "uploading", lastAttemptAt = now()) }
            token = tokenProvider(current.email)
            val drive = driveFactory(token)
            // Refuse an account mismatch even if Play services returned a token for a different default account.
            if (!drive.email().equals(current.email, true)) throw DriveFailure("La cuenta de Drive cambió", "Vuelve a conectar la cuenta mostrada en Ajustes.", authRequired = true)
            val latest = drive.backups(current.folderId).firstOrNull()
            if (latest != null && latest.id != state.value.baselineId && !acknowledgePending(latest)) {
                choice(latest); return@withLock SyncOutcome.NeedsAction
            }
            // A folder with a previously known backup becoming empty requires explicit confirmation as well.
            if (latest == null && state.value.baselineId.isNotEmpty()) {
                throw DriveFailure("La copia de Drive ya no está disponible", "La última copia pudo haberse movido o eliminado. Reconecta Drive para revisar el destino antes de subir otra copia.")
            }
            if (!force && backup.document.revision <= state.value.cloudRevision) {
                store.update { it.copy(phase = "synced", errorTitle = "", errorDetail = "", errorAt = 0) }; return@withLock SyncOutcome.Success
            }
            store.update { it.copy(pendingUploadId = backup.id, pendingUploadMd5 = backup.md5, pendingUploadRevision = backup.document.revision) }
            val uploaded = drive.upload(current.folderId, backup)
            require(uploaded.md5.equals(backup.md5, true) && uploaded.clientId == backup.id) { "Drive no confirmó la integridad del archivo subido." }
            store.update { it.copy(baselineId = uploaded.id, cloudRevision = backup.document.revision, lastCloudAt = uploaded.createdAt,
                pendingUploadId = "", pendingUploadMd5 = "", pendingUploadRevision = 0,
                errorTitle = "", errorDetail = "", errorAt = 0, phase = if (it.localRevision > backup.document.revision) "pending" else "synced") }
            try {
                drive.backups(current.folderId).drop(5).filter { it.id != uploaded.id }.forEach { drive.delete(it.id) }
            } catch (e: CancellationException) { throw e }
            catch (e: Exception) {
                store.update { it.copy(errorTitle = "Copia guardada; limpieza de versiones pendiente", errorDetail = detail(e), errorAt = now()) }
            }
            // Edits during upload remain dirty. WorkManager retries the newest committed revision.
            if (source.revision() > backup.document.revision) {
                refreshLocal()
                store.update { it.copy(phase = "pending") }
                SyncOutcome.Retry
            } else SyncOutcome.Success
        } catch (e: CancellationException) {
            store.update { if (it.phase == "uploading") it.copy(phase = "pending") else it }; throw e
        } catch (e: Exception) {
            if (e is DriveFailure && e.httpCode == 401 && token != null) {
                try { invalidateToken(token) } catch (canceled: CancellationException) { throw canceled } catch (_: Exception) { }
            }
            recordFailure(e)
            if (e is IOException || (e is DriveFailure && e.retryable)) SyncOutcome.Retry else SyncOutcome.NeedsAction
        }
    }

    suspend fun previewRestore(): RestorePreview = syncMutex.withLock {
        val current = state.value
        check(current.connected) { "Conecta Google Drive primero." }
        val drive = driveFactory(tokenProvider(current.email))
        check(drive.email().equals(current.email, true)) { "La cuenta de Google cambió. Reconecta Drive." }
        val file = drive.backups(current.folderId).firstOrNull() ?: error("No hay copias de FuelStatus en esta carpeta.")
        val bytes = drive.download(file.id)
        require(digest("MD5", bytes).equals(file.md5, true)) { "La descarga está incompleta. Intenta descargarla otra vez." }
        RestorePreview(current.email, file, BackupCodec.decode(bytes))
    }

    suspend fun restore(preview: RestorePreview) = syncMutex.withLock {
        require(preview.email.equals(state.value.email, true)) { "La cuenta cambió. Revisa la copia nuevamente." }
        store.update { it.copy(phase = "restoring") }
        try {
            localMutex.withLock {
                val before = source.snapshot(deviceId, now()).let { LocalBackup(it, BackupCodec.encode(it)) }
                local.preserveBeforeRestore(before)
                // The source validates and restores every table in one Room transaction.
                BackupCodec.decode(BackupCodec.encode(preview.document))
                source.restore(preview.document)
            }
            refreshLocal()
            store.update { it.copy(baselineId = preview.file.id, cloudRevision = 0, lastCloudAt = preview.file.createdAt,
                candidateId = "", candidateAt = 0, pendingUploadId = "", pendingUploadMd5 = "", pendingUploadRevision = 0,
                errorTitle = "", errorDetail = "", errorAt = 0, phase = "pending") }
            requestSync()
        } catch (e: CancellationException) { store.update { it.copy(phase = "pending") }; throw e }
        catch (e: Exception) { recordFailure(e); throw e }
    }

    /** Explicit confirmation to back up the current phone after another history has been discovered. */
    suspend fun useLocalHistory() = syncMutex.withLock {
        check(state.value.connected && state.value.needsChoice) { "No hay otro historial pendiente de revisión." }
        val current = state.value
        val drive = driveFactory(tokenProvider(current.email))
        check(drive.email().equals(current.email, true)) { "La cuenta de Google cambió. Reconecta Drive." }
        val latest = drive.backups(current.folderId).firstOrNull()
        require(latest?.id == current.candidateId) { "La copia de Drive cambió otra vez. Revisa el historial antes de continuar." }
        store.update { it.copy(baselineId = current.candidateId, candidateId = "", candidateAt = 0, cloudRevision = 0, phase = "pending") }
        requestSync(force = true)
    }

    private fun acknowledgePending(file: RemoteBackup): Boolean {
        val current = state.value
        if (current.pendingUploadId.isBlank() || file.clientId != current.pendingUploadId || !file.md5.equals(current.pendingUploadMd5, true)) return false
        store.update { it.copy(baselineId = file.id, cloudRevision = current.pendingUploadRevision, lastCloudAt = file.createdAt,
            pendingUploadId = "", pendingUploadMd5 = "", pendingUploadRevision = 0) }
        return true
    }

    private fun choice(file: RemoteBackup) = store.update { it.copy(candidateId = file.id, candidateAt = file.createdAt, phase = "review",
        errorTitle = "", errorDetail = "", errorAt = 0) }

    private fun recordFailure(error: Exception) = store.update {
        val title = when (error) {
            is DriveFailure -> error.title
            is IOException -> "No se pudo conectar con Google Drive"
            else -> "No se pudo completar el respaldo"
        }
        it.copy(phase = if (error is DriveFailure && error.authRequired) "auth_required" else "error",
            errorTitle = title, errorDetail = detail(error), errorAt = now())
    }

    private fun detail(error: Exception): String = when (error) {
        is DriveFailure -> error.detail
        else -> "${error.javaClass.simpleName}: ${error.message.orEmpty()}".take(4000)
    }
}
